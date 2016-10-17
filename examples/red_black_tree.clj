(require '[matchbox.matchers :as m])

; Based on http://www.leonardoborges.com/writings/2013/07/15/purely-functional-data-structures-in-clojure-red-black-trees/
(defn balance-vec [tree]
  (m/match tree
    (m/or (m/vector :black (m/vector :red (m/vector :red ?a ?x ?b) ?y ?c) ?z ?d)
          (m/vector :black (m/vector :red (m/vector :red ?a ?x ?b) ?y ?c) ?z ?d)
          (m/vector :black (m/vector :red ?a ?x (m/vector :red ?b ?y ?c)) ?z ?d)
          (m/vector :black ?a ?x (m/vector :red (m/vector :red ?b ?y ?c) ?z ?d))
          (m/vector :black ?a ?x (m/vector :red ?b ?y (m/vector :red ?c ?z ?d))))
    [:red [:black a x b] y [:black c z d]]

    _ tree))

(defn insert-vec [tree x]
  (let [ins (fn ins [tree]
              (m/match tree
                nil [:red nil x nil]
                (m/vector ?color ?a ?y ?b) (cond
                                             (< x y) (balance-vec [color (ins a) y b])
                                             (> x y) (balance-vec [color a y (ins b)])
                                             :else tree)))
        [_ a y b] (ins tree)]
    [:black a y b]))

(defn tree-vec->list [tree]
  (m/match tree
    nil nil
    (m/vector _ ?left ?value ?right) (concat (tree-vec->list left) (cons value (tree-vec->list right)))))

(-> nil
    (insert-vec 10)
    (insert-vec 25)
    (insert-vec 1)
    (insert-vec 4)
    (insert-vec 40)
    (insert-vec 12)
    (insert-vec 21)
    tree-vec->list)
(reduce insert-vec nil (range 20))

; With Records
(require '[matchbox.matchers :as m])
(defrecord Black [left value right])
(defrecord Red [left value right])
(defn balance-rec [tree]
  (m/match tree
    (m/or (m/record Black (m/record Red (m/record Red ?a ?x ?b) ?y ?c) ?z ?d)
          (m/record Black (m/record Red (m/record Red ?a ?x ?b) ?y ?c) ?z ?d)
          (m/record Black (m/record Red ?a ?x (m/record Red ?b ?y ?c)) ?z ?d)
          (m/record Black ?a ?x (m/record Red (m/record Red ?b ?y ?c) ?z ?d))
          (m/record Black ?a ?x (m/record Red ?b ?y (m/record Red ?c ?z ?d))))
    (->Red (->Black a x b) y (->Black c z d))

    _ tree))

(defn insert-rec [tree x]
  (let [ins (fn ins [tree]
              (m/match tree
                nil (->Red nil x nil)

                (m/matches {:left ?a :value ?y :right ?b})
                (cond
                  (< x y) (balance-rec (assoc tree :left (ins a)))
                  (> x y) (balance-rec (assoc tree :right (ins b)))
                  :else tree)))
        {:keys [left value right]} (ins tree)]
    (->Black left value right)))

(defn tree-rec->list [tree]
  (m/match tree
    nil nil

    (m/matches {:left ?a :value ?y :right ?b})
    (concat (tree-rec->list a) (cons y (tree-rec->list b)))))

(-> nil
    (insert-rec 10)
    (insert-rec 25)
    (insert-rec 1)
    (insert-rec 4)
    (insert-rec 40)
    (insert-rec 12)
    (insert-rec 21)
    tree-rec->list)
(reduce insert-rec nil (range 20))
