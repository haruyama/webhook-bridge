(ns webhook-bridge.input.cloudhost
  (:require [clojure.data.json]))
; {"object_kind":"issue","object_attributes": {"id":934,"title":"hoge","assignee_id":35,"author_id":35,"project_id":1731,"created_at":"2014-06-07T05:32:32.742Z","updated_at":"2014-06-07T05:32:32.742Z","position":0,"branch_name":null,"description":"ueue","milestone_id":null,"state":"opened","iid":1}}

(defmulti format-input (fn [params options] (params "object_kind")))

(defmethod format-input "issue" [params options]
  (let [attributes (params "object_attributes")
        state      (attributes "state")
        title      (attributes "title")
        iid        (attributes "iid")
        project    (options "project")
        ]
    {:subject (str "Issue #" iid " " title " " state)
     :body (str "https://git.cloudhost.io/" project "/issues/" iid) }))

(defn input [body options]
  (with-open [isr (java.io.InputStreamReader. body)]
    (format-input (clojure.data.json/read isr) options)))
