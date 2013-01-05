(ns clojuresubs.utils
  ;*CLJSBUILD-REMOVE*;(:require goog.string.format)
  (:require;*CLJSBUILD-REMOVE*;-macros
    [clojuresubs.macros :as macros]))

(defn parse-int
  "Parse string into integer (platform-dependent implementation)."
  [s]
  (macros/if-clojurescript
    (js/parseInt s)
    (Integer/parseInt s)))

(defn my-format
  "printf-style formatting, returns string (platform-dependent implementation)."
  [fmt & args]
  (macros/if-clojurescript
    (apply goog.string.format  fmt args)
    (apply clojure.core/format fmt args)))

(defn partition-sections
  "Partitions coll into subsequences. New subsequence is started
  whenever pred returns truthy value. Initial falsey values are
  dropped. Returns lazy seq."
  [pred coll]
  (let [co-pred  (complement pred)
        [x & xs] (drop-while co-pred coll)]
    (when x
      (cons (concat [x] (take-while co-pred xs))
            (lazy-seq (partition-sections pred xs))))))
