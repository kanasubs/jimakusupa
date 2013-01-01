(ns clojuresubs.subtitles)

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

(def default-info
  {"WrapStyle" "0"
   "PlayResX"  "640"
   "PlayResY"  "480"
   "ScaledBorderAndShadow" "yes"})

(def default-event
  {:Marked false
   :Layer 0
   :Start 0
   :End 10000
   :Style "Default"
   :Name ""
   :MarginL 0
   :MarginR 0
   :MarginV 0
   :Effect ""
   :Text ""})

(def default-style
  {:Fontname "Arial"
   :Fontsize 20.0
   :PrimaryColour [255 255 255 0] 
   :SecondaryColour [255 0 0 0]
   :OutlineColour [0 0 0 0]
   :TertiaryColour [0 0 0 0]
   :BackColour [0 0 0 0]
   :Bold false
   :Italic false
   :Underline false
   :StrikeOut false
   :ScaleX 100.0
   :ScaleY 100.0
   :Spacing 0.0
   :Angle 0.0
   :BorderStyle 1
   :Outline 2.0
   :Shadow 2.0
   :Alignment 2
   :MarginL 10
   :MarginR 10
   :MarginV 10
   :AlphaLevel 0
   :Encoding 1})

(def default-subtitles
  "Instance of Subtitles with default style and metadata and no events."
  (Subtitles. default-info
              {"Default" default-style}
              []))

(defn dialogue-event?
  "Predicate whether given map represents Dialogue event." 
  [event]
  (= (event :Type) :Dialogue))
