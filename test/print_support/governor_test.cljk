(ns print-support.governor-test
  (:require [clojure.test :refer [deftest is testing]]
            [print-support.store :as store]
            [print-support.governor :as governor]))

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "shop-1" :name "Acme Bindery" :equipment-scope :cutting})
    st))

(deftest ok-on-clean-job-log
  (let [st (fresh-store)
        proposal {:op :log-service-job :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:client-id "shop-1"} {} proposal st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest ok-on-maintenance-schedule
  (let [st (fresh-store)
        proposal {:op :schedule-maintenance :effect :propose :confidence 0.85 :stake :medium}
        v (governor/check {:client-id "shop-1"} {} proposal st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (not (:escalate? v)))))

(deftest hard-on-unregistered-client
  (let [st (fresh-store)
        proposal {:op :log-service-job :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:client-id "no-such-shop"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-client (:rule %)) (:violations v)))))

(deftest hard-on-no-direct-actuation-violation
  (let [st (fresh-store)
        proposal {:op :log-service-job :effect :direct-write :confidence 0.9 :stake :low}
        v (governor/check {:client-id "shop-1"} {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-direct-actuation (:rule %)) (:violations v)))))

(deftest escalates-on-quality-defect
  (let [st (fresh-store)
        proposal {:op :flag-quality-defect :effect :propose :confidence 0.9 :stake :high}
        v (governor/check {:client-id "shop-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest escalates-on-delivery-coordination
  (let [st (fresh-store)
        proposal {:op :coordinate-delivery :effect :propose :confidence 0.9 :stake :high}
        v (governor/check {:client-id "shop-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest escalates-on-low-confidence
  (let [st (fresh-store)
        proposal {:op :log-service-job :effect :propose :confidence 0.2 :stake :low}
        v (governor/check {:client-id "shop-1"} {} proposal st)]
    (is (:escalate? v))
    (is (not (:hard? v)))))

(deftest store-records-and-ledger-append-only
  (let [st (fresh-store)]
    (store/commit-record! st {:client-id "shop-1" :op :log-service-job})
    (store/append-ledger! st {:disposition :commit})
    (is (= 1 (count (store/jobs-of st "shop-1"))))
    (is (= 1 (count (store/ledger st))))))
