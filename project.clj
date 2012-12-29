(defproject clojuresubs "0.0.1"
  :description "A Clojure library for working with subtitles"
  :url "https://github.com/tigr42/clojuresubs"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]]
  :plugins [[lein-cljsbuild "0.2.10"]]
  :cljsbuild {:builds [{:source-path "src-cljs"
                        :compiler {:output-to "javascript/clojuresubs.js"
                                   :optimizations :whitespace
                                   :pretty-print true}}]
              :crossovers [clojuresubs.core
                           clojuresubs.utils]
              :crossover-path "src-cljs"})
