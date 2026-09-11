(ns print-support.actor-test
  (:require [clojure.test :refer [deftest is testing]]
            [print-support.store :as store]
            [print-support.advisor :as advisor]
            [print-support.actor :as actor]))

(defn- fresh-setup []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "shop-1" :name "Acme Bindery"})
    {:store st :graph (actor/build-graph {:store st})}))

(deftest actor-commits-clean-job-log
  (let [{:keys [store graph]} (fresh-setup)
        result (actor/run-request! graph
                                    {:client-id "shop-1" :op :log-service-job :stake :low}
                                    {}
                                    "thread-1")]
    (is (= :done (:status result)))
    (is (= 1 (count (store/jobs-of store "shop-1"))))))

(deftest actor-escalates-quality-defect
  (let [{:keys [store graph]} (fresh-setup)
        result (actor/run-request! graph
                                    {:client-id "shop-1" :op :flag-quality-defect :stake :high}
                                    {}
                                    "thread-2")]
    (is (= :interrupted (:status result)))
    (is (= 0 (count (store/jobs-of store "shop-1"))))))

(deftest actor-holds-on-unregistered-client
  (let [{:keys [store graph]} (fresh-setup)
        result (actor/run-request! graph
                                    {:client-id "unknown-shop" :op :log-service-job :stake :low}
                                    {}
                                    "thread-3")]
    (is (= :done (:status result)))
    (is (= 0 (count (store/jobs-of store "unknown-shop"))))))

(deftest actor-appends-ledger-on-all-paths
  (let [{:keys [store graph]} (fresh-setup)]
    (actor/run-request! graph
                        {:client-id "shop-1" :op :log-service-job :stake :low}
                        {}
                        "thread-4")
    (is (> (count (store/ledger store)) 0))))
