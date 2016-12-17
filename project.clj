(defproject cloth "0.4.0-SNAPSHOT"
  :description "Clojure(Script) tools for Ethereum"
  :url "https://github.com/pelle/cloth"
  :dependencies [[org.clojure/clojure "1.9.0-alpha8"]
                 [org.clojure/clojurescript "1.9.293"]
                 [org.clojure/core.async "0.2.395"]
                 [funcool/cats "2.0.0"]
                 [funcool/promesa "1.6.0"]
                 [funcool/httpurr "0.6.2"]
                 [aleph "0.4.1" :scope "provided"]
                 [funcool/cuerdas "2.0.1"]
                 [org.ethereum/ethereumj-core "1.2.0-RELEASE"]
                 [clj-time "0.12.2"]
                 [com.andrewmcveigh/cljs-time "0.4.0"]
                 [byte-streams "0.2.2"]
                 [secp256k1 "1.0.3"]
                 [com.cemerick/url "0.1.1"]
                 ;[cljsjs/bignumber "2.1.4-1"]
                 [cheshire "5.6.3"]
                 [funcool/octet "1.0.1"]]
  :jvm-opts ^:replace ["-Xmx1g" "-server"]
  :plugins [[lein-npm "0.6.1"]
            [lein-cljsbuild "1.1.3"]
            [lein-figwheel "0.5.4-7"]
            [lein-doo "0.1.7"]
            [lein-codox "0.9.5"]
            [lein-ancient "0.6.10"]
            [lein-externs "0.1.5"]]
  :npm {:dependencies [[karma "1.3.0"]
                       [karma-chrome-launcher "2.0.0"]
                       [karma-cljs-test "0.1.0"]
                       [derequire "2.0.3"]
                       [browser-builds "pelle/browser-builds#babelify"]]}

  ;; NOT WORKING
  :aliases {"build-ethjs" ^{:doc "Build version of ethereumjs-tx"}
                          ["shell"
                           "cat" "node_modules/browser-builds/dist/ethereumjs-tx.js" "|" "derequire"
                                  ">src/ethereumjs-tx/ethereumjs-tx.js"]}
  :profiles {:dev {:plugins [[lein-auto "0.1.2"]
                             [lein-shell "0.5.0"]]}}
  :cljsbuild
  {:builds {:dev      {:source-paths ["src"]
                       :figwheel     true
                       :compiler     {:optimizations :none
                                      :main          cloth.core
                                      :asset-path "js/out"
                                      :output-to "resources/public/js/cloth.js"
                                      :output-dir "resources/public/js/out"}}
            :test     {:source-paths ["src" "test"]
                       :compiler     {:output-to     "out/testable.js"
                                      :main          cloth.runner
                                      :source-map    true
                                      :optimizations :none}}
            :advanced {:source-paths ["src" "test"]
                       :compiler     {:output-to     "out/testable.js"
                                      :main          "cloth.do-runner"
                                      :optimizations :advanced}}}}
  :doo {:build "test"}
  :source-paths ["src" "target/classes"]
  :clean-targets ["out" "release" "target"]
  :auto {:default {:file-pattern #"\.(clj|cljs|cljc|cljx|edn|sol)$"}
         :paths [ "src" "test"]}
  ;; Home of ethereumj
  :repositories [["oss.jfrog.org" "http://dl.bintray.com/ethereum/maven"]])

