(ns jdubie.secret-santa.core
  (:require [clojure.java.shell :as shell]))

(defn lookup
  [{:keys [first-name last-name]}]
  (shell/sh "osascript" "-e"
      (format "tell application \"Contacts\" to set p to first person whose (first name is \"%s\" and last name is \"%s\")"
              first-name
              last-name)))

(defn all-contacts-exist?
  [contacts]
  (->> contacts
       (map lookup)
       (every? (fn [{:keys [exit]}] (= 0 exit)))))

(defn send!
  [{:keys [first-name last-name message]}]
  (let [{:keys [exit err] :as response}
        (shell/sh "osascript" "-e" (format "tell application \"Messages\" to send \"%s\" to buddy \"%s %s\""
                                           message first-name last-name))]
    (when (not= exit 0)
      (if (re-find #"send a message to yourself" err)
        (println message)
        (throw (ex-info "iMessage error" response))))))


(comment

  ;; step 0. test sending a message
  (send! {:message "Test message"
          :first-name "..."
          :last-name "..."})

  ;; step 1. set group contact list - these need to exist verbatim in MacOS Contacts
  (def contacts "TODO this in" [{:first-name "..."
                                 :last-name "...."}])

  ;; step 2. make sure all contacts exist
  (assert (all-contacts-exist? contacts))

  ;; step 3. create assignments and messages for each member to recieve
  (def assignments
    (let [shuffled (shuffle contacts)]
      (->> (interleave shuffled (concat (rest shuffled) [(first shuffled)]))
           (partition 2)
           (mapcat (fn [[santa reciever]]
                     [(merge santa
                             {:message "\uD83E\uDD2B\uD83C\uDF85\uD83C\uDFFC"})
                      (merge santa
                             {:message (format "Welcome to secret santa. This year you have %s."
                                               (:first-name reciever))})])))))

  ;; step 4. send out assignments
  (->> assignments
       (map send!)
       (doall)))