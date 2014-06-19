(ns jimakusupa.core
  (:use jimakusupa.util)
  (:require [clojure.string :as string]))

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

(def subrip-timestamp-line #" *(\d{2}:\d{2}:\d{2}[,.]\d{3}) *--> *(\d{2}:\d{2}:\d{2}[,.]\d{3})(?: .*)?")

(defn- parse-subrip-by-lines
  [lines]
  (let [timestamp-line?   (partial re-matches subrip-timestamp-line)
        my-string-replace #(string/replace %3 %1 %2)]
    (for [[[timestamp-line] [& text-lines]] (->> lines
                                                 (drop-while (complement timestamp-line?))   
                                                 (partition-by timestamp-line?)
                                                 (partition 2))
          :let [[start end] (->> timestamp-line
                                 (re-matches subrip-timestamp-line)
                                 rest
                                 (map decode-subrip-timestamp))
                text        (->> text-lines
                                 (string/join "\n")
                                 (my-string-replace #"(?m)^ *\d+ *$" "")
                                 string/trim
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

(defmulti parse (fn [ext _] (if (= ext :srt) :srt :ssa)))

(defmethod parse :srt [_ text] (parse-subrip text))

(defmethod parse :ssa [_ text]
  ; https://github.com/JDaren/subtitleConverter/blob/master/src/main/java/subtitleFile/Caption.java
  (if text
    (let [parsed-sub (.parseFile (subtitleFile.FormatASS.) "" (str->stream text))
          get-mseconds #(get-field (class %) "mseconds" %)
          eventλ #(identity {:Start (-> % .getValue .start get-mseconds)
                             :End (-> % .getValue .end get-mseconds)
                             :Text (-> % .getValue .content)})]
      {:events (vec (map eventλ (.captions parsed-sub)))})))

(defmethod parse :default [_ text])