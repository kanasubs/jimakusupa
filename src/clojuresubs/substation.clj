(ns clojuresubs.substation
  "Functions related to SubStation Alpha (ASS) subtitle format."
  (:require [clojure.string :as string])
  (:use [clojuresubs.utils :only [parse-int
                                  my-format
                                  partition-sections]]
        
        [clojuresubs.subtitles :only [default-event
                                      default-subtitles
                                      dialogue-event?]]))

;; FIXME

;; ## style/event fields

(def ass-event-format [:Layer  :Start :End :Style :Name :MarginL :MarginR :MarginV :Effect :Text])
(def ssa-event-format [:Marked :Start :End :Style :Name :MarginL :MarginR :MarginV :Effect :Text])

(def ass-style-format [:Fontname :Fontsize
                       :PrimaryColour :SecondaryColour :OutlineColour :BackColour
                       :Bold :Italic :Underline :StrikeOut
                       :ScaleX :ScaleY :Spacing :Angle
                       :BorderStyle
                       :Outline :Shadow
                       :Alignment
                       :MarginL :MarginR :MarginV
                       :Encoding])
(def ssa-style-format [:Fontname :Fontsize
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

(defn get-style-line-format
  [version]
  (case version
    :ass ass-style-format
    :ssa ssa-style-format))

;; ------------------------------------------------------------------------------------------------
;; field decoders

(def substation-timestamp
  "H:MM:SS.cc"
  #"(\d):(\d{2}):(\d{2}).(\d{2})")

(defn decode-substation-timestamp
  [timestamp]
  {:export true} 
  (let [[h m s cs] (->> timestamp (re-matches substation-timestamp) rest (map parse-int))]
    (+ (* 10 cs) (* 1000 s) (* 60 1000 m) (* 60 60 1000 h))))

;; TODO: more

;; ------------------------------------------------------------------------------------------------
;; field encoders

(defn encode-substation-timestamp
  [total-ms]
  {:export true
   :pre [(not (neg? total-ms))
         (< total-ms (* 10 60 60 1000))]}
  (let [cs (rem (quot total-ms 10) 100)
        s  (rem (quot total-ms 1000) 60)
        m  (rem (quot total-ms (* 60 1000)) 60)
        h  (quot total-ms (* 60 60 1000))]
    (my-format "%d:%02d:%02d.%02d" h m s cs)))

;; TODO: more

;; ------------------------------------------------------------------------------------------------
;; parser

;; FIXME

(def substation-section-header #"\[[^\]]+\]")
(def ordinary-substation-line #"(.+?) *: *(.+)")

(defn parse-substation-section
  [subtitles [header & lines]]
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
  (let [section-header? (partial re-matches substation-section-header)
        useful-line?    (complement #(or (empty? %) (= (first %) \;)))
        filtered-lines  (->> lines (map string/trim) (filter useful-line?))
        sections        (partition-sections section-header? filtered-lines)]
    (reduce parse-substation-section empty-subtitles sections)))

(defn parse-substation
  [text]
  (parse-substation-lines (string/split-lines text)))


;; ------------------------------------------------------------------------------------------------
;; serializer

;; TODO
