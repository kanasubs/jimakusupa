;*CLJSBUILD-MACRO-FILE*;
(ns clojuresubs.macros)

(defmacro if-clojurescript
  [then else]
  (if (contains? (->> (all-ns) (map ns-name) set) 'cljs.core)
    then
    else))
