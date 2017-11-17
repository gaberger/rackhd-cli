(defproject rackhd-cli "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.zalando/swagger1st "0.25.0"]
                 [ring "1.6.1"]
                 [http-kit "2.2.0"]
                 [cheshire "5.8.0"]
                 [lambdaisland/uri "1.1.0"]
                 [org.clojure/tools.cli "0.3.5"]]

  :main dell-taskgraph-cli.core
  :aot [dell-taskgraph-cli.core])
