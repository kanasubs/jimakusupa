(ns clojuresubs.core
  (:require [clojure.string :as string])
  (:use [clojuresubs.utils :only [parse-int enumerate my-format]]))

(defprotocol ISubtitles
  (shift [_ ms-delta] "Shift all timestamps in file by ms-delta (clipping at zero). Returns new instance."))

(defrecord Subtitles [info styles events]
  ISubtitles
  (shift
    [_ ms-delta]
    (let [shift-line (fn [{start :Start end :End :as fields}]
                         (assoc fields :Start (max (+ start ms-delta) 0)
                                       :End   (max (+ end   ms-delta) 0)))]
      (Subtitles. info styles (map shift-line events)))))

(def empty-subtitles (Subtitles. {} {} []))

(defn ^:export encode-subrip-timestamp
  [total-ms]
  {:pre [(not (neg? total-ms))
         (< total-ms (* 100 60 60 1000))]}
  (let [ms (rem total-ms 1000)
        s  (rem (quot total-ms 1000) 60)
        m  (rem (quot total-ms (* 60 1000)) 60)
        h  (quot total-ms (* 60 60 1000))]
    (my-format "%02d:%02d:%02d,%03d" h m s ms)))

(defn ^:export encode-substation-timestamp
  [total-ms]
  {:pre [(not (neg? total-ms))
         (< total-ms (* 10 60 60 1000))]}
  (let [cs (rem (quot total-ms 10) 100)
        s  (rem (quot total-ms 1000) 60)
        m  (rem (quot total-ms (* 60 1000)) 60)
        h  (quot total-ms (* 60 60 1000))]
    (my-format "%d:%02d:%02d.%02d" h m s cs)))

(defn ^:export decode-subrip-timestamp
  [timestamp]
  (let [[h m s ms] (->> timestamp
                        (re-matches #"(\d{2}):(\d{2}):(\d{2})[,.](\d{3})")
                        rest
                        (map parse-int))]
    (+ ms (* 1000 s) (* 60 1000 m) (* 60 60 1000 h))))

(defn ^:export decode-substation-timestamp
  [timestamp]
  (let [[h m s cs] (->> timestamp
                        (re-matches #"(\d):(\d{2}):(\d{2}).(\d{2})")
                        rest
                        (map parse-int))]
    (+ (* 10 cs) (* 1000 s) (* 60 1000 m) (* 60 60 1000 h))))

(defn ^:export encode-subrip-text
  [text]
  (-> text
      (string/replace #"\{[^\}]*\}" "")
      (string/replace "\\h" " ")
      (string/replace #"\\[nN]" "\n")))

(defn ^:export decode-subrip-text
  [text]
  (-> text
      (string/replace "\n" "\\N")
      (string/replace #"<([^>]+)>" "")))

(def subrip-timestamp-line #"(\d{2}:\d{2}:\d{2}[,.]\d{3}) *--> *(\d{2}:\d{2}:\d{2}[,.]\d{3})(?: .*)?")

(defn- parse-subrip-by-lines
  [lines]
  (let [timestamp-line?   (partial re-matches subrip-timestamp-line)
        my-string-replace #(string/replace %3 %1 %2)]
    (for [[[timestamp-line] [& text-lines]] (->> lines
                                                 string/trim
                                                 (drop-while (complement timestamp-line?))   
                                                 (partition-by timestamp-line?)
                                                 (partition 2))
          :let [[start end] (->> timestamp-line
                                 (re-matches subrip-timestamp-line)
                                 rest
                                 (map decode-subrip-timestamp))
                text        (->> text-lines
                                 (string/join "\n")
                                 (my-string-replace #"\n\d+$" "") ;; note: no lookbehind in js
                                 decode-subrip-text)]]
      {:Start start :End end :Text text})))

(defn ^:export parse-subrip-lines
  [lines]
  (Subtitles. [] [] (parse-subrip-by-lines lines)))

(defn ^:export parse-subrip
  [text]
  (parse-subrip-lines (string/split-lines text)))

(defn ^:export serialize-subrip
  [subtitles]
  (string/join "\n\n" (for [[i event] (enumerate 1 (:events subtitles))]
                        (format "%d\n%s --> %s\n%s"
                          i
                          (encode-subrip-timestamp (:Start event))
                          (encode-subrip-timestamp (:End event))
                          (encode-subrip-text      (:Text event))))))

(def substation-section-header #"\[[^\]]+\]")
(def ordinary-substation-line #"(.+?) *: *(.+)")

(def ass-event-format [:Layer :Start :End :Style :Name :MarginL :MarginR :MarginV :Effect :Text])
(def ass-style-format [:Name :Fontname :Fontsize
                       :PrimaryColour :SecondaryColour :OutlineColour :BackColour
                       :Bold :Italic :Underline :StrikeOut
                       :ScaleX :ScaleY :Spacing :Angle
                       :BorderStyle
                       :Outline :Shadow
                       :Alignment
                       :MarginL :MarginR :MarginV
                       :Encoding])

(def ssa-event-format [:Marked :Start :End :Style :Name :MarginL :MarginR :MarginV :Effect :Text])
(def ass-style-format [:Name :Fontname :Fontsize
                       :PrimaryColour :SecondaryColour :TertiaryColour :BackColour
                       :Bold :Italic
                       :BorderStyle
                       :Outline :Shadow
                       :Alignment
                       :MarginL :MarginR :MarginV
                       :AlphaLevel
                       :Encoding])

(defn get-event-line-format
  [version]
  (case version
    :ass ass-event-format
    :ssa ssa-event-format))

(defn decode-substation-field
  [version field-name data]
  nil)

(defn parse-substation-section
  [subtitles header lines]
  (let [line-to-kv       #(rest (re-matches ordinary-substation-line %))
        omit-format-line (partial filter (fn [[event-type _]] (not= event-type "Format")))
        version          (case (get-in subtitles [:info "ScriptType"])
                           "v4.00+" :ass "v4.00" :ssa :unknown)]
    (do
      (case header
        "[Script Info]" (let [info (apply hash-map (flatten (map line-to-kv lines)))]
                          (Subtitles. (conj (:info subtitles) info)
                                      (:styles subtitles)
                                      (:events subtitles)))
        "[V4 Styles]"   subtitles
        "[V4+ Styles]"  subtitles
        "[Events]"      subtitles ;(let [decode-field (partial decode-substation-field version)
                        ;      field-names  (get-event-line-format version)
                        ;      raw-data     (for [[event-type fields] (omit-format-line (map line-to-kv lines))]
                        ;                        (conj {:Type event-type} (zipmap field-names
                        ;                                                         (string/split fields #"," (count field-names)))))]
                        ;  (Subtitles. (:info subtitles)
                        ;              (:styles subtitles)
                        ;              (conj (:events subtitles) raw-data)))) ;; XXX 
        subtitles))))

(defn parse-substation-lines
  [lines]
  (let [useful-line?        (complement #(or (empty? %) (= (first %) \;)))
        not-section-header? (complement (partial re-matches substation-section-header))]
    (loop [subtitles empty-subtitles
           lines     (->> lines
                          (map string/trim)
                          (filter useful-line?)
                          (drop-while not-section-header?))]
      (let [header-line     (first lines)
            section-lines   (take-while not-section-header? (rest lines))
            remaining-lines (drop-while not-section-header? (rest lines))]
        (if (seq lines)
          (recur (parse-substation-section subtitles header-line section-lines) remaining-lines)
          subtitles)))))

(defn parse-substation
  [text]
  (parse-substation-lines (string/split-lines text)))
