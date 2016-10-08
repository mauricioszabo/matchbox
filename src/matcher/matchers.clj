(ns matcher.matchers
  (:refer-clojure :exclude [list vector instance? map pmap])
  (:require [clojure.core.unify :as u]
            [matcher.utils.match :as utils]))

(defn- check-elements [m rest]
  (let [[before after] (split-with #(not= % '&) rest)
        m-size (count m)
        b-size (count before)
        b-vec (vec before)
        m-vec (vec m)]
    (cond
      (empty? after) (when (= m-size b-size) [b-vec m-vec])
      (= 2 (count after)) (when (>= m-size b-size)
                            [(conj b-vec (last after))
                             (conj (subvec m-vec 0 b-size) (drop b-size m-vec))])
      :else nil)))

(defn list [ & rest]
  (fn [m] (when (list? m) (check-elements m rest))))

(defn vector [ & rest]
  (fn [m] (when (vector? m) (check-elements m rest))))

(defn matches [pattern]
  (fn [obj] [pattern obj]))

; (defn map [ & pattern]
;   (fn [obj]
;     (when (and (map? obj))
;       (let [forms (for [p (partition 2 pattern)
;                         o obj
;                         :let [match (utils/match-and-unify o (matches p))]
;                         :when match]
;                     [p o])]
;         (when (not-empty forms)
;           (reduce (fn [[lss rss] [ls rs]] [(conj lss ls) (conj rss rs)]) [] forms))))))

(let [s (set {:a 10 :b 20})
      m (some #{[:a 10]} s)]
  (disj s m))
(flatten (seq{:a 10 :b 20}))

; ((m/map '?a (m/list & '_)) {:a 10, :b 20, :c '(1 2 3)})
; (utils/apply-match :a '?a)
; (utils/apply-match '(1 2 3) (list '& '_))

(defn- match-kv [elem [pattern-k pattern-v]]
  (let [[k v] elem
        k-match (utils/apply-match k pattern-k)
        v-match (utils/apply-match v pattern-v)
        uni #(some-> % (apply u/unify k-match))]
    (println elem pattern-k pattern-v)
    (println k-match v-match)
    (when (and (apply u/unify k-match) (apply u/unify v-match))
      (let [[ls-k rs-k] k-match
            [ls-v rs-v] v-match]
        [elem [ls-k ls-v] [rs-k rs-v]]))))

(defn map [ & pattern]
  (fn [obj]
    (when (and (map? obj) (even? (count pattern)))
      (loop [[pattern & forms] (partition 2 pattern)
             map (set obj)
             [ls rs] [[] []]]
        (if pattern
          (let [[m l r] (some #(match-kv % pattern) map)
                submap (disj map m)
                acc [(conj ls l) (conj rs r)]]
            (and m (recur forms submap acc)))
          [ls rs])))))


(defn instance? [class]
  (fn [obj]
    (if (and (clojure.core/instance? java.lang.Class class)
             (clojure.core/instance? class obj))
      [])))

(defmacro match [obj & matches]
  (apply utils/match* obj matches))

(u/unify {'?a '?b} {:a 10, :b 20})
