(defproject neurovoice "0.1.0-SNAPSHOT"
  :description "NeuroVoice - Aplikacija za analizu glasovnih signala"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.12.2"]]
  :main neurovoice.core
  :profiles {:dev
             {:dependencies [[midje "1.10.10"]]
              :plugins [[lein-midje "3.2.1"]]}}
  :repl-options {:init-ns neurovoice.core})