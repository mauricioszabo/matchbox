(ns matcher.matchers
  (:refer-clojure :exclude [list])
  (:require [clojure.core.unify :as u]))

(defn list [m & rest]
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

(defn- gen-if-let [obj [[match-fn & args] then & rest]]
  (let [sym (gensym)
        match-fn (cons match-fn (cons obj args))
        post (if (empty? rest)
               `(throw (IllegalArgumentException. "No match"))
               (gen-if-let obj rest))]
    `(if-let [~sym ~match-fn]
       ~then
       ~post)))

(defmacro match [obj & matches]
  (println matches)
  (assert (even? (count matches)) "Matches must be even")
  (gen-if-let obj matches))

(defmacro extract-vals [obj [matcher & args]]
  (let [is-bind? (fn [x] (and (symbol? x) (-> x name (.startsWith "?"))))
        norm (map (fn [x] (if (is-bind? x)
                            `'~x
                            x))
                  args)]
    `(let [res# ~(cons matcher (cons obj norm))]
       (some->> res#
                (reduce (fn [acc# [var# val#]]
                          (if (and (symbol? var#) (-> var# name (.startsWith "?")))
                            (let [var-name# (->> var#
                                                 name
                                                 (drop 1)
                                                 (apply str)
                                                 symbol)]
                              (update acc#
                                      (if (some #(-> % first (= var-name#)) (:bindings acc#))
                                        :vals
                                        :bindings)
                                      #(conj (vec %) [var-name# val#])))
                            (update acc# :vals #(conj (vec %) [var# val#]))))
                        {})))))

; (u/unify ['?a 20 '_ 10] [0 20 30 10])
; (u/unify [10 20 30 10] [0 20 30 10])
; (u/unify [10 '?b] [10 '(10)])
