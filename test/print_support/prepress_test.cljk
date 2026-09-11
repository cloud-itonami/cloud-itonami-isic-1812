(ns print-support.prepress-test
  "Prepress craft wire: seihan.core/plan is the only geometry source;
  blocking findings become governor hard holds."
  (:require [clojure.test :refer [deftest is testing]]
            [print-support.prepress :as prepress]
            [print-support.store :as store]
            [print-support.governor :as governor]
            [print-support.actor :as actor]
            [seihan.core :as seihan]))

(def ^:private a4-cmyk
  {:pages 16
   :colors #{:c :m :y :k}
   :paper-mm [210 297]
   :bleed-mm 3
   :trap-mm 0.1})

(defn- fresh-store []
  (let [st (store/mem-store)]
    (store/register-client! st {:client-id "shop-1"
                                :name "Acme Prepress"
                                :equipment-scope :prepress})
    st))

;; ---------------------------------------------------------------- craft module

(deftest plan-delegates-geometry-to-seihan
  (testing "plates + imposition are seihan's, not invented here"
    (let [wire (prepress/plan a4-cmyk)
          direct (seihan/plan a4-cmyk)]
      (is (true? (:ok? wire)))
      (is (= (:plates direct) (get-in wire [:plan :plates])))
      (is (= (:imposition direct) (get-in wire [:plan :imposition])))
      (is (= (seihan/summary direct) (:summary wire)))
      (is (empty? (:violations wire))))))

(deftest plan-is-deterministic
  (is (= (prepress/plan a4-cmyk) (prepress/plan a4-cmyk))))

(deftest blocking-findings-become-violations
  (testing "zero pages"
    (let [r (prepress/plan (assoc a4-cmyk :pages 0))]
      (is (false? (:ok? r)))
      (is (seq (:violations r)))
      (is (every? #(= :prepress-blocking-finding (:rule %)) (:violations r)))
      (is (some #{:zero-pages} (map :kind (:violations r))))))
  (testing "missing paper size"
    (let [r (prepress/plan (dissoc a4-cmyk :paper-mm))]
      (is (false? (:ok? r)))
      (is (some #{:missing-paper-size} (map :kind (:violations r))))))
  (testing "no colors"
    (let [r (prepress/plan (assoc a4-cmyk :colors #{}))]
      (is (false? (:ok? r)))
      (is (some #{:no-colors} (map :kind (:violations r))))))
  (testing "negative bleed"
    (let [r (prepress/plan (assoc a4-cmyk :bleed-mm -1))]
      (is (false? (:ok? r)))
      (is (some #{:negative-bleed} (map :kind (:violations r)))))))

(deftest non-blocking-findings-do-not-hold
  (testing "pages not multiple of 4 is a warning, not a hard hold"
    (let [r (prepress/plan (assoc a4-cmyk :pages 6 :binding :saddle-stitch))]
      (is (true? (:ok? r)))
      (is (empty? (:violations r)))
      (is (some #{:pages-not-multiple-of-4}
                (map :kind (get-in r [:plan :findings])))))))

(deftest empty-job-does-not-invent-geometry
  (let [r (prepress/plan {})]
    (is (false? (:ok? r)))
    (is (empty? (get-in r [:plan :plates]))
        "no plates invented when job is empty")
    (is (nil? (get-in r [:plan :imposition]))
        "no imposition invented when paper/pages missing")
    (is (seq (:violations r)))))

(deftest plan-ops-recognized
  (is (prepress/plan-op? :plan-prepress))
  (is (prepress/plan-op? :prepress/plan))
  (is (not (prepress/plan-op? :log-service-job))))

;; ---------------------------------------------------------------- governor

(deftest governor-holds-on-blocking-prepress
  (let [st (fresh-store)
        proposal {:op :plan-prepress
                  :effect :propose
                  :confidence 0.95
                  :stake :low
                  :job (assoc a4-cmyk :pages 0)}
        v (governor/check {:client-id "shop-1"} {} proposal st)]
    (is (:hard? v))
    (is (not (:ok? v)))
    (is (some #(= :prepress-blocking-finding (:rule %)) (:violations v)))
    (is (some #(= :zero-pages (:kind %)) (:violations v)))
    (is (false? (get-in v [:prepress :ok?])))))

(deftest governor-ok-on-clean-prepress
  (let [st (fresh-store)
        proposal {:op :plan-prepress
                  :effect :propose
                  :confidence 0.95
                  :stake :low
                  :job a4-cmyk}
        v (governor/check {:client-id "shop-1"} {} proposal st)]
    (is (:ok? v))
    (is (not (:hard? v)))
    (is (true? (get-in v [:prepress :ok?])))
    (is (= 4 (get-in v [:prepress :summary :plate-count])))
    (is (= [210.0 297.0]
           (get-in v [:prepress :plan :imposition :trim-mm]))
        "trim-mm comes from seihan imposition, not invented")))

(deftest governor-job-may-live-on-request
  (let [st (fresh-store)
        proposal {:op :prepress/plan :effect :propose :confidence 0.9 :stake :low}
        request {:client-id "shop-1" :job (dissoc a4-cmyk :colors)}
        v (governor/check request {} proposal st)]
    (is (:hard? v))
    (is (some #(= :no-colors (:kind %)) (:violations v)))))

(deftest governor-non-prepress-ops-unchanged
  (let [st (fresh-store)
        proposal {:op :log-service-job :effect :propose :confidence 0.9 :stake :low}
        v (governor/check {:client-id "shop-1"} {} proposal st)]
    (is (:ok? v))
    (is (nil? (:prepress v)))))

;; ---------------------------------------------------------------- actor end-to-end

(deftest actor-holds-blocking-prepress-no-ssot-write
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        result (actor/run-request!
                graph
                {:client-id "shop-1"
                 :op :plan-prepress
                 :stake :low
                 :job {:pages 0
                       :colors #{:k}
                       :paper-mm [100 100]
                       :bleed-mm 0
                       :trap-mm 0}}
                {}
                "prepress-hold")]
    (is (= :done (:status result)))
    (is (= :hold (get-in result [:state :disposition])))
    (is (= 0 (count (store/jobs-of st "shop-1")))
        "no job record on hard hold")
    (is (some #{:hold} (map :disposition (store/ledger st))))
    (is (some #(= :prepress-blocking-finding (:rule %))
              (get-in result [:state :verdict :violations])))))

(deftest actor-commits-clean-prepress-with-seihan-summary
  (let [st (fresh-store)
        graph (actor/build-graph {:store st})
        result (actor/run-request!
                graph
                {:client-id "shop-1"
                 :op :plan-prepress
                 :stake :low
                 :job a4-cmyk}
                {}
                "prepress-ok")
        jobs (vec (store/jobs-of st "shop-1"))]
    (is (= :done (:status result)))
    (is (= :commit (get-in result [:state :disposition])))
    (is (= 1 (count jobs)))
    (is (= :plan-prepress (:op (first jobs))))
    (is (true? (:prepress-ok? (first jobs))))
    (is (= "seihan.core/plan" (:source (first jobs))))
    (is (= 4 (get-in (first jobs) [:prepress-summary :plate-count])))
    (is (= 16 (get-in (first jobs) [:prepress-summary :pages])))))
