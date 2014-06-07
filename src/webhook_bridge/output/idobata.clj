(ns webhook-bridge.output.idobata
  (:require [clj-http.lite.client :as client]))

(defn output [url message]
  (clj-http.lite.client/post url {:body  (str "source=" (message :subject) "\n" (message :body))}))
