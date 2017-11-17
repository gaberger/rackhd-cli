(ns rackhd-cli.core
  (:require
    [org.httpkit.client :as http]
    [lambdaisland.uri :refer [uri join]]
    [cheshire.core :refer :all]
    [clojure.pprint :as pprint]
    [clojure.string :as str]
    [clojure.edn :as edn]
    [clojure.tools.cli :refer [parse-opts]])
  (:gen-class)
  (:import (java.net InetAddress)))



(def base-url "/api/2.0")

(def api
  {:tasks     (str/join "/" [base-url "workflows/tasks"])
   :nodes     (str/join "/" [base-url "nodes"])
   :graphs    (str/join "/" [base-url "workflows/graphs"])
   :workflows (str/join "/" [base-url "workflows" []])})


(defn get-url [api]
  (let [response (http/get api)
        status (:status @response)
        body (:body @response)]
    (condp = status
      200 (decode body true)
      (throw (Exception. (str (:status @response)))))))


(def cli-options
  [
   ["-p" "--port PORT" "Port number"
    :default 9005
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 0 % 0x10000) "Must be a number between 0 and 65536"]]
   ["-H" "--hostname HOST" "Remote host"
    :default "localhost"
    :default-desc "localhost"]
   ["-v" nil "Verbosity level; may be specified multiple times to increase value"
    :id :verbosity
    :default 0
    :assoc-fn (fn [m k _] (update-in m [k] inc))]
   ["-h" "--help"]])

(defn usage [options-summary]
  (->> ["on-taskgraph CLI"
        ""
        "Usage: dell_taskgraph_cli [options] action"
        ""
        "Options:"
        options-summary
        ""
        "Actions:"
        "  list     List a service"
        "  get      Get a workflow, task"
        "  create   Create a workflow or task"
        ""
        "Please refer to the manual page for more information."]
       (str/join \newline)))


(defn sub-command-usage [options-summary]
  (->> ["usage: list tasks"
        "       list graphs"
        "       get tasks  <instanceId>"
        "       get graphs <instanceId>"
        ""
        "Options:"
        options-summary
        ""]
       (str/join \newline)))


(defn error-msg [errors]
  (str "The following errors occurred while parsing your command:\n\n"
       (str/join \newline errors)))

(defn validate-main-args
  "Validate command line arguments. Either return a map indicating the program
  should exit (with a error message, and optional ok status), or a map
  indicating the action the program should take and the options provided."
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options :in-order true)]
    (cond
      (:help options) {:exit-message (usage summary) :ok? true}
      errors
      {:exit-message (error-msg errors)}
      (#{"list" "get" "create"} (first arguments))
      {:action (first arguments) :options options :arguments (next arguments)}
      :else                                                 ; failed custom validation => exit with usage summary
      {:exit-message (usage summary)})))


(defn validate-list-args
  [args]
  (let [{:keys [options arguments errors summary]} (parse-opts args cli-options :in-order true)]
    (cond
      (:help options) {:exit-message (usage summary) :ok? true}
      errors
      {:exit-message (error-msg errors)}
      (#{"tasks" "graphs"} (first arguments))
      {:action (first arguments) :options options :arguments (next arguments)}
      :else                                                 ; failed custom validation => exit with usage summary
      {:exit-message (sub-command-usage summary)})))


(defn exit [status msg]
  (println msg)
  #_(System/exit status))

(defn list-command [uri]
  (let [result (get-url uri)]
    (pprint/print-table (map #(select-keys % [:instanceId :_status :friendlyName :injectableName]) result))))

(defn list-handler [args]
  (let [{:keys [action options arguments exit-message ok?]} (validate-list-args args)]
       (if exit-message
          (exit (if ok? 0 1) exit-message)
          (let [path (api (keyword action))
                uri (str (assoc (uri "") :scheme "http" :host (:hostname options) :port (:port options) :path path))]
            (case action
              "tasks" (list-command uri)
              "graphs" (list-command uri)
              (println (str "Error shouldnt be here")))))))

(defn get-command [uri]
  (let [result (get-url uri)]
    ;(pprint/pprint (map #(select-keys % [:instanceId :_status :friendlyName :injectableName]) result))))
    (pprint/pprint result)))
;
;(defn get-handler [args options]
;  (let [arg (first args)
;        resource (fnext args)
;        path  (api (keyword arg))
;        uri (str (assoc (uri "") :scheme "http" :host (:hostname options) :port (:port options) :path  (str/join "/" [path resource])))]
;    (case arg
;      "tasks" (get-command uri)
;      "graphs" (get-command uri)
;      (println (str "Error: " arg " not found")))))


(defn get-handler [args]
  (let [{:keys [action options arguments exit-message ok?]} (validate-list-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (let [path (api (keyword action))
            resource (first arguments)
            uri (str (assoc (uri "") :scheme "http" :host (:hostname options) :port (:port options) :path path  :path  (str/join "/" [path resource])))]
        (case action
          "tasks" (get-command uri)
          "graphs" (get-command uri)
          (println (str "Error shouldnt be here")))))))


(defn -main [& args]
  (let [{:keys [action options arguments exit-message ok?]} (validate-main-args args)]
    (if exit-message
      (exit (if ok? 0 1) exit-message)
      (case action
        "list" (list-handler arguments)
        "get"  (get-handler arguments)
        "create" (str "create " options)))))