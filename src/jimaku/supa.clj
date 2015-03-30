(ns jimaku.supa
  (:use jimaku.util)
  (:require [clojure.string :as str]))

(defprotocol ISubtitles
  (shift [_ ms-delta]
    (str "Shift all timestamps in file by ms-delta (clipping at zero). "
         "Returns new instance.")))

(defrecord Subtitles [info styles events]
  ISubtitles
  (shift
    [_ ms-delta]
    (let [shift-line (fn [{start :Start end :End :as fields}]
                         (assoc fields :Start (max (+ start ms-delta) 0)
                                       :End   (max (+ end   ms-delta) 0)))]
      (Subtitles. info styles (map shift-line events)))))

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

(defn ^:export decode-subrip-timestamp [timestamp]
  (let [[h m s ms] (->> timestamp
                        (re-matches #"(\d{2}):(\d{2}):(\d{2})[,.](\d{3})")
                        rest
                        (map parse-int))]
    (+ ms (* 1000 s) (* 60 1000 m) (* 60 60 1000 h))))

(defn ^:export decode-substation-timestamp [timestamp]
  (let [[h m s cs] (->> timestamp
                        (re-matches #"(\d):(\d{2}):(\d{2}).(\d{2})")
                        rest
                        (map parse-int))]
    (+ (* 10 cs) (* 1000 s) (* 60 1000 m) (* 60 60 1000 h))))

(defn ^:export encode-subrip-text
  [text]
  (-> text
    (str/replace #"\{[^\}]*\}" "")
    (str/replace "\\h" " ")
    (str/replace #"\\[nN]" "\n")))

(defn ^:export decode-subrip-text
  [text]
  (-> text
    (str/replace "\n" "\\N")
    (str/replace #"<([^>]+)>" "")))

(def subrip-timestamp-line #" *(\d{2}:\d{2}:\d{2}[,.]\d{3}) *--> *(\d{2}:\d{2}:\d{2}[,.]\d{3})(?: .*)?")

(defn- parse-subrip-by-lines [lines]
  (let [timestamp-line?   (partial re-matches subrip-timestamp-line)
        my-string-replace #(str/replace %3 %1 %2)]
    (for [[[timestamp-line] [& text-lines]] (->> lines
                                                 (drop-while (complement timestamp-line?))   
                                                 (partition-by timestamp-line?)
                                                 (partition 2))
          :let [[start end] (->> timestamp-line
                                 (re-matches subrip-timestamp-line)
                                 rest
                                 (map decode-subrip-timestamp))
                text        (->> text-lines
                                 (str/join "\n")
                                 (my-string-replace #"(?m)^ *\d+ *$" "")
                                 str/trim
                                 decode-subrip-text)]]
      {:Start start :End end :Text text})))

(defn ^:export parse-subrip-lines [lines]
  (Subtitles. [] [] (parse-subrip-by-lines lines)))

(defn ^:export parse-subrip [text]
  (parse-subrip-lines (str/split-lines text)))

(defn ^:export serialize-subrip [subtitles]
  (str/join
    "\n\n"
    (map-indexed #(format "%d\n%s --> %s\n%s"
                   (inc %1)
                   (encode-subrip-timestamp (:Start %2))
                   (encode-subrip-timestamp (:End %2))
                   (encode-subrip-text (:Text %2)))
                 (:events subtitles))))

(defmulti parse
  (fn [ext _]
    (let [ext (->> ext name (take-last 3) str/join)]
      (if (= ext "srt")
        "srt"
        "ssa"))))

(defmethod parse "srt" [_ text] (parse-subrip text))

(defmethod parse "ssa" [_ text]
  ; https://github.com/JDaren/subtitleConverter/blob/master/src/main/java/subtitleFile/Caption.java
  (if text
    (let [parsed-sub (.parseFile (subtitleFile.FormatASS.) "" (str->stream text))
          get-mseconds #(get-field (class %) "mseconds" %)
          eventλ #(identity {:Start (-> % .getValue .start get-mseconds)
                             :End (-> % .getValue .end get-mseconds)
                             :Text (-> % .getValue .content)})]
      {:events (vec (map eventλ (.captions parsed-sub)))})))

(defmethod parse :default [_ text])

(defn srt-txt?
  "Returns true if the text is in format srt, and false otherwise."
  [text] (= (first text) \1))
