(ns clojuresubs.subrip
  "Functions related to SubRip Text (SRT) subtitle format."
  (:require [clojure.string :as string])
  (:use [clojuresubs.utils     :only [parse-int
                                      my-format
                                      partition-sections]]      
        [clojuresubs.subtitles :only [default-event
                                      default-subtitles
                                      dialogue-event?]]))

;; ## Field decoders

(def timestamp
  "HH:MM:SS,mmm"
  #"(\d{2}):(\d{2}):(\d{2})[,.](\d{3})")

(defn decode-timestamp
  [timestamp]
  {:export true} 
  (let [[h m s ms] (->> timestamp (re-matches timestamp) rest (map parse-int))]
    (+ ms (* 1000 s) (* 60 1000 m) (* 60 60 1000 h))))

(defn- decode-tags
  [s]
  (let [f (fn [[_ slash tag]]
            (cond
              (#{"i" "b" "u" "s"} tag) (str "{\\\\" tag (if slash 0 1) "}")
              :else ""))]
    (string/replace #"<(/)?([^>]+)>" f)))

(defn decode-text
  [text]
  {:export true}
  (-> text
      decode-tags  
      (string/replace "\n" "\\N")))


;; ## Field encoders

(defn encode-timestamp
  [total-ms]
  {:pre [(not (neg? total-ms))
         (< total-ms (* 100 60 60 1000))]
   :export true}
  (let [ms (rem total-ms 1000)
        s  (rem (quot total-ms 1000) 60)
        m  (rem (quot total-ms (* 60 1000)) 60)
        h  (quot total-ms (* 60 60 1000))]
    (my-format "%02d:%02d:%02d,%03d" h m s ms)))

(defn encode-text
  [text]
  {:export true}
  ;; TODO: encode basic tags (inline and from style)
  (-> text
      (string/replace #"\{[^\}]*\}" "")
      (string/replace "\\h" " ")
      (string/replace #"\\[nN]" "\n")
      string/trim))


;; ## Parser

(def timestamp-line
  "HH:MM:SS,mmm --> HH:MM:SS,mmm"
  #"(\d{2}:\d{2}:\d{2}[,.]\d{3}) *--> *(\d{2}:\d{2}:\d{2}[,.]\d{3})(?: .*)?")

(defn- parse-event
  "Builds SubStation Event map from timestamp and text lines."
  [[timestamps & body]]
  ;; Note: since we are splitting the file at timecode lines, which are preceeded by subtitle number,
  ;; body should end with an empty line and the number of next subtitle, all of which should be stripped.  
  (let [[start end] (map decode-timestamp (rest (re-matches timestamp-line timestamps)))
        next-subtitle-number #"\n{2,}\d+\n*$"
        text (decode-text (-> (string/join "\n" body) (string/replace next-subtitle-number "")))]
    (assoc default-event :Start start :End end :Text text)))

(defn parse-lines
  "Reads SubRip (SRT) file from sequence of lines, returning Subtitle instance."
  [lines]
  {:export true}
  (let [timestamp-line? (partial re-matches timestamp-line)
        events          (partition-sections timestamp-line? (map string/trim lines))]
    (assoc default-subtitles :events (mapv parse-event events))))

(defn parse
  "Reads SubRip (SRT) file from a string, returning Subtitle instance.
  If you're reading from an actual file, you may want to use parse-subrip-lines instead." 
  [s]
  {:export true} 
  (parse-lines (string/split-lines s)))


;; ## Serializer

(defn- serialize-event
  "Prints one Dialogue event into a string, in SubRip (SRT) format."
  [i event]
  {:pre [(dialogue-event? event)
         (pos? i)]}
  ;; TODO: how to propagate style information to text encoder?
  (let [start (encode-timestamp (:Start event))
        end   (encode-timestamp (:End event))
        text  (encode-text      (:Text event))]
    (my-format "%d\n%s --> %s\n%s" i start end text)))

(defn serialize
  "Prints Subtitles instance into a string, in SubRip (SRT) format.
  Only Dialogue events are output; Comment events are not.
  Only basic formatting is retained (eg. newlines, italics)."
  [subtitles]
  {:export true}
  (string/join "\n\n" (map serialize-event
                           (map inc (range))
                           (filter dialogue-event? (:events subtitles)))))
