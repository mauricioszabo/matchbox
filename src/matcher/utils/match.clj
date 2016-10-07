(ns matcher.utils.match
  (:require [clojure.core.unify :as u]))

(defn parse-args [[fun & args]]
  (let [parsed (map (fn [a]
                      (if (and (symbol? a) (->> a name (re-find #"^[\?_]")))
                        `(~'quote ~a)
                        a))
                    args)]
    (cons fun parsed)))

(declare apply-match)
(defn- recurse-into-result [possible-fn obj]
  (if (fn? possible-fn)
    (apply-match obj possible-fn)
    [possible-fn obj]))

(defn- apply-inner [[ls rs]]
  (when (= (count ls) (count rs))
    (loop [[first-ls & rest-ls] ls
           [first-rs & rest-rs] rs
           [acc-ls acc-rs] [[] []]]

      (when-let [inner-res (recurse-into-result first-ls first-rs)]
        (let [[ls rs] inner-res
              acc [(conj acc-ls ls) (conj acc-rs rs)]]
          (if (empty? rest-ls)
            acc
            (recur rest-ls rest-rs acc)))))))

(defn apply-match [obj match-fn]
  (let [res (match-fn obj)]
    (if (vector? res)
      (apply-inner res)
      nil)))
