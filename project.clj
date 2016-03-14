(defproject cloth "0.1.0-SNAPSHOT"
  :description "ClojureScript tools for Ethereum"
  :url "http://example.com/FIXME"
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [funcool/cats "1.2.1"]
                 [funcool/promesa "0.8.1"]
                 [funcool/httpurr "0.4.0"]]
  :jvm-opts ^:replace ["-Xmx1g" "-server"]
  :plugins [[lein-npm "0.6.1"]
            [lein-cljsbuild "1.1.3"]
            [lein-doo "0.1.6"]]
  :npm {:dependencies [[source-map-support "0.4.0"]
                       [ethereumjs-tx "1.1.1"]]}

  :cljsbuild {:test
                 {:source-paths ["src/cljc" "src/cljs" "test/cljs"]
                  :compiler
                                {:output-to "target/test.js"
                                 :main "cloth.doo-runner"
                                 :optimizations :whitespace
                                 :pretty-print true}}}
  :source-paths ["src" "target/classes"]
  :clean-targets ["out" "release"]
  :target-path "target")
