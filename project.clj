(defproject jimakusupa "0.1.2"

  :description "A Clojure library for working with subtitles"

  :url "https://github.com/kanasubs/jimakusupa"

  :scm {:name "git" :url "https://github.com/kanasubs/jimakusupa"}

  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :repositories
    [["subtitleConverter"
      "https://oss.sonatype.org/content/groups/public/com/github"]]

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [riccardove.easyjasub/subtitleConvert "1.0.2"]])