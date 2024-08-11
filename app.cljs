(ns app
  (:require ["moment$default" :as moment]
            ["inquirer$default" :as inquirer]
            [promesa.core :as p]
            ["mongodb$default" :as mongodb]
            [nbb.core :refer [await]]))



(def password (or js/process.env.BMAN_DB_PASS "HHDR8nLQPZPR1cGc"))
(def user (or js/process.env.BMAN_DB_USER "huzaifa"))



(def url (str "mongodb+srv://" user ":" password "@cluster0.dg1vr.mongodb.net/?retryWrites=true&w=majority&appName=Cluster0"))

(def client (new mongodb/MongoClient url))

(defn write-birthday [name day month]
  (p/let [conn (.connect client)
          db (.db conn "birthdaydb")
          collection (.collection db "birthdates")
          response (.insertOne collection #js {:name name
                                               :day (str day)
                                               :month month})
          _ (.close conn)]
    response))

(comment
  (await (write-birthday "tom" 21 "january"))
  (await (write-birthday "tony" 8 "august"))
  (await (write-birthday "huzaifa" 11 "august")))


(def questions (clj->js [{:name "name"
                          :type "input"
                          :message "whos birthday is it?"}
                         {:name "day"
                          :type "number"
                          :message "whars day is your birthday?"
                          :validate (fn [v]
                                      (<= 1 v 31))}
                         {:name "month"
                          :type "list"
                          :choices (moment/months)}]))

(defn create-birthday-entry []
  (p/let [_answers (inquirer/prompt questions)
          answers (js->clj _answers :keywordize-keys true)
          {:keys [name day month]} answers]
    (prn "saving birthday for" name day month)
    (write-birthday name day month)))




(defn find-birthday-entries [day month]
  (println "finding birthdays" day month)
  (p/let [query #js {:month month
                     :day (str day)}
          conn (.connect client)
          db (.db conn "birthdaydb")
          collection (.collection db "birthdates")
          response (.toArray (.find collection query))
          _ (.close conn)]
    response))


(defn list-birthdays []
  (p/let [month (.format (moment) "MMMM")
          day (.date (moment))
          _entries (find-birthday-entries day month)
          entries (js->clj _entries :keywordize-keys true)]
    (run! (fn [{:keys [name]}]
            (println "It's" (str name "'s") "birtday today 🎂")) entries)))



(cond
  (= (first *command-line-args*) "list") (list-birthdays)
  :else (create-birthday-entry))