(ns jimaku.util)

(defn parse-int [s]
  "Parse string into integer (platform-dependent implementation)."
  (Integer/parseInt s))

(defn my-format [fmt & args]
  "printf-style formatting, returns string (platform-dependent implementation)."
  (apply clojure.core/format fmt args))

(defn enumerate
  "Returns a lazy sequence of elems in coll and their indices,
  ie. [i elem] vectors. Indices count from zero (by default)."
  ([i coll]
    (when (seq coll) (cons [i (first coll)] (lazy-seq (enumerate (inc i) (rest coll))))))
  ([coll]
    (enumerate 0 coll)))

(defn str->stream [string] (-> string .getBytes clojure.java.io/input-stream))

(defn get-field
  "Access to private or protected field.  field-name is a symbol or
  keyword."
  [klass field-name obj]
  (-> klass (.getDeclaredField (name field-name))
      (doto (.setAccessible true))
      (.get obj)))