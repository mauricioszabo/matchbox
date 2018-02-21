(ns matcher.matchers
  (:refer-clojure :exclude [list vector instance? map pmap or and])
  (:require [clojure.core :as core]
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

(defn coll [ & rest]
  (fn [m] (when (coll? m) (check-elements m rest))))

(defn matches [pattern]
  (fn [obj] [pattern obj]))

(defn satisfies
  ([pred-fn]
   (fn [obj]
     (try
       (when (pred-fn obj)
         [])
       (catch Throwable _))))
  ([pred-fn bind]
   (fn [obj]
     (try
       (when-let [res (pred-fn obj)]
         [[bind] [res]])
       (catch Throwable _)))))

(defn- check-map-pattern [obj map]
  [[] []])

(defn map [ & pattern]
  (fn [obj]
    (when (core/and (map? obj) (even? (count pattern)))
      (check-map-pattern obj (into {} (->> pattern
                                           (partition 2)
                                           (mapv vec)
                                           (into {})))))))

(defn regexp [re & binds]
  (fn [str]
    (when (string? str)
      (when-let [[_ & rest] (re-find re str)]
        (if (-> binds count zero?)
          [[] []]
          [(vec binds) (vec rest)])))))

(defn instance [class]
  (fn [obj]
    (if (core/and (core/instance? java.lang.Class class)
                  (core/instance? class obj))
      [])))

(defn or [ & matchers]
  (fn [obj]
    (some #(if-let [r (utils/match-and-unify obj %)]
             [(keys r) (vals r)]) matchers)))

(defn and [ & matchers]
  (fn [obj]
    (let [applied (core/map #(utils/match-and-unify obj %) matchers)]
      (when (every? map? applied)
        (let [ks (core/map keys applied)
              vs (core/map vals applied)]
          [ks vs])))))

(defn- normalize-clj-name [name]
  (-> name
    (clojure.string/replace #"^(.+\.)?" "$1->")
    (clojure.string/replace #"^(.+)\." "$1/")
    (clojure.string/replace #"_BANG_" "!")
    (clojure.string/replace #"_STAR_" "*")
    (clojure.string/replace #"_PLUS_" "+")
    (clojure.string/replace #"_GT_" ">")
    (clojure.string/replace #"_GTE_" ">=")
    (clojure.string/replace #"_LT_" "<")
    (clojure.string/replace #"_LTE_" "<=")
    (clojure.string/replace #"(eval\d+\$|__\d+)" "")
    (clojure.string/replace #"_" "-")
    (clojure.string/replace #"_" "-")))

(defmacro record [record & args]
  (let [arglist (when (symbol? record)
                  (some-> record name normalize-clj-name
                          symbol resolve meta :arglists first
                          (->> (core/map keyword) vec)))
        arguments (vec args)]
    `(fn [~'obj]
       (when (core/and ~arglist
                  (core/instance? ~record ~'obj)
                  (core/or (= (count ~arguments) 0)
                           (= (count ~arguments) (count ~arglist))))
         (if (zero? (count ~arguments))
           [[] []]
           [~arguments (core/map #(get ~'obj %) ~arglist)])))))

(defmacro match [obj & matches]
  (apply utils/match* obj matches))
