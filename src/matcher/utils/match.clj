(ns matcher.utils.match
  (:require [clojure.core.unify :as u]
            [clojure.string :as str]))

(defn parse-args [[fun & args]]
  (let [parsed (map (fn [a]
                      (cond
                        (and (symbol? a) (->> a name (re-find #"^[\?_]"))) `(~'quote ~a)
                        (coll? a) (parse-args a)
                        :else a))
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

(defn- create-let [unbound-vars sym then]
  (let [bindings (->> unbound-vars
                      set
                      (mapcat (fn [v] [(-> v name (str/replace #"^\?" "") symbol)
                                       `('~v ~sym)]))
                      vec)]
    `(let ~bindings ~then)))

(defn match-and-unify [obj match-fn]
  (some->> (apply-match obj match-fn)
           (apply u/unify)))

(defn wrap-let [obj match-fn then else]
  (let [var (gensym)
        norm-fn (parse-args match-fn)
        unbound-vars (filter #(if (symbol? %) (-> % name (.startsWith "?"))) (flatten match-fn))
        let-clause (if (empty? unbound-vars)
                     then
                     (create-let unbound-vars var then))]
    `(if-let [~var (match-and-unify ~obj ~norm-fn)]
       ~let-clause
       ~else)))

(defn match* [obj & matches]
  (assert (even? (count matches)) "Matches must be even")
  (let [[match-fn then & rest] matches
        else (cond
               (= match-fn '_) then
               (empty? rest) `(throw (IllegalArgumentException. "No match"))
               :else (apply match* obj rest))]
    (if (= match-fn '_)
      else
      (wrap-let obj match-fn then else))))
