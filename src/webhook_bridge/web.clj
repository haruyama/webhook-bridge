(ns webhook-bridge.web
  (:require [compojure.core :refer [defroutes GET PUT POST DELETE ANY]]
            [compojure.handler :refer [site]]
            [compojure.route :as route]
            [clojure.java.io :as io]
            [ring.middleware.stacktrace :as trace]
            [ring.middleware.session :as session]
            [ring.middleware.session.cookie :as cookie]
            [ring.adapter.jetty :as jetty]
            [ring.middleware.basic-authentication :as basic]
            [webhook-bridge.output.idobata]
            [webhook-bridge.input.cloudhost]
            [environ.core :refer [env]]))

(def HOOKS
  {"tokyo.clj" {
          :input  webhook-bridge.input.cloudhost/input
          :output webhook-bridge.output.idobata/output
          :output_endpoint "https://idobata.io/hook/4167dc8e-7da8-4d41-ada7-5e1701ed2ffa"
          :options {"project" "haruyama/webhooktest" }
          }
   })

(defroutes app
  (GET "/" []
       {:status 200
        :headers {"Content-Type" "text/plain"}
        :body (pr-str ["Hello" :from 'Heroku])})
  (POST "/hook/:hook" {body :body {hook-key :hook} :params}
        (let [hook (get HOOKS hook-key)]
          (if (not hook)
            (route/not-found (slurp (io/resource "404.html")))
            (do
              ((hook :output)
               (hook :output_endpoint)
               ((hook :input) body (hook :options)))
              {:status 200
               :headers {"Content-Type" "text/plain"}
               :body (pr-str hook-key)})
            )))
  (ANY " *" []
       (route/not-found (slurp (io/resource "404.html")))))

(defn wrap-error-page [handler]
  (fn [req]
    (try (handler req)
         (catch Exception e
           {:status 500
            :headers {"Content-Type" "text/html"}
            :body (slurp (io/resource "500.html"))}))))

(defn wrap-app [app]
  ;; TODO: heroku config:add SESSION_SECRET=$RANDOM_16_CHARS
  (let [store (cookie/cookie-store {:key (env :session-secret)})]
    (-> app
        ((if (env :production)
           wrap-error-page
           trace/wrap-stacktrace))
        (site {:session {:store store}}))))

(defn -main [& [port]]
  (let [port (Integer. (or port (env :port) 5000))]
    (jetty/run-jetty (wrap-app #'app) {:port port :join? false})))

;; For interactive development:
;; (.stop server)
;; (def server (-main))
