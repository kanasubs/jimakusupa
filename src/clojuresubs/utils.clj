(ns clojuresubs.utils
  ;*CLJSBUILD-REMOVE*;(:require goog.string.format)
  (:require;*CLJSBUILD-REMOVE*;-macros
    [clojuresubs.macros :as macros]))

(defn parse-int [s]
  "Parse string into integer (platform-dependent implementation)."
  (macros/if-clojurescript
    (js/parseInt s)
    (Integer/parseInt s)))

(defn my-format [fmt & args]
  "printf-style formatting, returns string (platform-dependent implementation)."
  (macros/if-clojurescript
    (apply goog.string.format  fmt args)
    (apply clojure.core/format fmt args)))

(defn enumerate
  "Returns a lazy sequence of elems in coll and their indices,
  ie. [i elem] vectors. Indices count from zero (by default)."
  ([i coll]
    (when (seq coll) (cons [i (first coll)] (lazy-seq (enumerate (inc i) (rest coll))))))
  ([coll]
    (enumerate 0 coll)))