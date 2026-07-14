(ns demolition.facts-test
  (:require [clojure.test :refer [deftest is]]
            [demolition.facts :as facts]))

(deftest jpn-has-a-spec-basis
  (is (some? (facts/spec-basis "JPN")))
  (is (string? (:hazmat-survey-provenance (facts/spec-basis "JPN"))))
  (is (= :quantitative (:threshold-model (facts/spec-basis "JPN"))))
  (is (= 7 (:notification-lead-days (facts/spec-basis "JPN")))))

(deftest usa-has-a-spec-basis-with-a-different-numeric-lead-time
  (is (= :quantitative (:threshold-model (facts/spec-basis "USA"))))
  (is (= 10 (:notification-lead-days (facts/spec-basis "USA")))))

(deftest deu-is-honestly-qualitative-not-fabricated
  (is (= :qualitative (:threshold-model (facts/spec-basis "DEU"))))
  (is (nil? (:notification-lead-days (facts/spec-basis "DEU")))))

(deftest unknown-jurisdiction-has-no-fabricated-spec-basis
  (is (nil? (facts/spec-basis "ATL"))))

(deftest coverage-never-reports-a-missing-jurisdiction-as-covered
  (let [report (facts/coverage ["JPN" "ATL" "USA"])]
    (is (= 2 (:covered report)))
    (is (= ["ATL"] (:missing-jurisdictions report)))
    (is (= ["JPN" "USA"] (:covered-jurisdictions report)))))

;; ----------------------------- notification-lead-insufficient? -----------------------------

(deftest jpn-lead-time-is-a-real-numeric-recheck
  (is (true? (facts/notification-lead-insufficient? "JPN" {:notification-lead-days-actual 2})))
  (is (false? (facts/notification-lead-insufficient? "JPN" {:notification-lead-days-actual 7})))
  (is (false? (facts/notification-lead-insufficient? "JPN" {:notification-lead-days-actual 10}))))

(deftest usa-lead-time-uses-its-own-different-numeric-minimum
  (is (true? (facts/notification-lead-insufficient? "USA" {:notification-lead-days-actual 8})))
  (is (false? (facts/notification-lead-insufficient? "USA" {:notification-lead-days-actual 10}))))

(deftest deu-never-gets-a-fabricated-true-false
  (is (= :qualitative (facts/notification-lead-insufficient? "DEU" {:notification-lead-days-actual 100})))
  (is (= :qualitative (facts/notification-lead-insufficient? "DEU" {:notification-lead-days-actual 0}))))

(deftest unknown-jurisdiction-returns-nil-not-a-guess
  (is (nil? (facts/notification-lead-insufficient? "ATL" {:notification-lead-days-actual 100}))))

(deftest non-numeric-actual-never-fires-a-quantitative-hold
  (is (false? (facts/notification-lead-insufficient? "JPN" {:notification-lead-days-actual nil}))))

;; ----------------------------- catalog citation honesty -----------------------------

(deftest jpn-cites-real-hazmat-survey-and-notification-law
  (let [sb (facts/spec-basis "JPN")]
    (is (re-find #"石綿障害予防規則" (:hazmat-survey-basis sb)))
    (is (re-find #"laws\.e-gov\.go\.jp" (:hazmat-survey-provenance sb)))
    (is (re-find #"建設リサイクル法|再資源化" (:demolition-notification-basis sb)))
    (is (re-find #"laws\.e-gov\.go\.jp" (:demolition-notification-provenance sb)))))

(deftest usa-cites-real-osha-and-neshap-law
  (let [sb (facts/spec-basis "USA")]
    (is (re-find #"1926\.1101" (:hazmat-survey-basis sb)))
    (is (re-find #"osha\.gov" (:hazmat-survey-provenance sb)))
    (is (re-find #"61\.145" (:demolition-notification-basis sb)))
    (is (re-find #"ecfr\.gov" (:demolition-notification-provenance sb)))
    (is (re-find #"model code" (:permit-note sb)) "honestly labeled a model code, not federal statute")))

(deftest deu-cites-real-eu-directive-and-german-transposition
  (let [sb (facts/spec-basis "DEU")]
    (is (re-find #"2009/148" (:hazmat-survey-basis sb)))
    (is (re-find #"eur-lex\.europa\.eu" (:hazmat-survey-provenance sb)))
    (is (re-find #"TRGS 519|GefStoffV|Gefahrstoffverordnung" (:hazmat-survey-basis sb)))))

(deftest uncovered-jurisdiction-has-no-fabricated-catalog-entry
  (is (nil? (facts/spec-basis "ATL"))))
