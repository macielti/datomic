(defproject net.clojars.macielti/datomic "1.0.0"
  :description "Datomic"
  :url "https://github.com/macielti/datomic"

  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}

  :dependencies [[org.clojure/clojure "1.12.4"]
                 [com.taoensso/timbre "6.8.0"]
                 [diehard "0.12.0"]
                 [com.datomic/peer "1.0.7556"]
                 [integrant "1.0.1"]]

  :profiles {:dev {:resource-paths ^:replace ["test/resources"]

                   :test-paths     ^:replace ["test/unit" "test/integration" "test/helpers"]

                   :plugins        [[com.github.clojure-lsp/lein-clojure-lsp "2.0.14"]
                                    [com.github.liquidz/antq "RELEASE"]]

                   :dependencies   [[nubank/matcher-combinators "3.10.0"]
                                    [prismatic/schema "1.4.1"]
                                    [hashp "0.2.2"]]

                   :injections     [(require 'hashp.core)]

                   :aliases        {"clean-ns"     ["clojure-lsp" "clean-ns" "--dry"] ;; check if namespaces are clean
                                    "format"       ["clojure-lsp" "format" "--dry"] ;; check if namespaces are formatted
                                    "diagnostics"  ["clojure-lsp" "diagnostics"]
                                    "lint"         ["do" ["clean-ns"] ["format"] ["diagnostics"]]
                                    "clean-ns-fix" ["clojure-lsp" "clean-ns"]
                                    "format-fix"   ["clojure-lsp" "format"]
                                    "lint-fix"     ["do" ["clean-ns-fix"] ["format-fix"]]}
                   :repl-options   {:init-ns common-clj.schema.core}}}

  :repl-options {:init-ns datomic.component}

  :resource-paths ["resources"])
