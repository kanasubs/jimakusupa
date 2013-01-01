(defproject clojuresubs "0.0.1-SNAPSHOT"
  :description "A Clojure library for working with subtitles"
  :url "https://github.com/tigr42/clojuresubs"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]]
  :plugins [[lein-cljsbuild "0.2.10"]]
  :hooks [leiningen.cljsbuild]
  :cljsbuild {:builds [{:source-path "src-cljs"
                        :jar true
                        :compiler {:output-to "javascript/clojuresubs.js"
                                   :optimizations :none
                                   :pretty-print true}}]
              :crossovers [clojuresubs.core
                           clojuresubs.utils]
              :crossover-path "src-cljs"
              :crossover-jar false})
