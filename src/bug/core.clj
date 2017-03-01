(ns bug.core)

(def the-map {:a 2})

(defn related? [protocol x]
  {:extends?   (extends? protocol (type x))
   :satisfies? (satisfies? protocol x)}
  )

;; Now eval this:

(related? clojure.core.protocols/IKVReduce the-map)

;; Problem is that Clojure 1.7 gives:

{:extends? true, :satisfies? true}

;; but Clojure 1.8 gives:

{:extends? false, :satisfies? true}

;; What gives?



(defn super-chain [^Class c]
  (when c
    (cons c (super-chain (.getSuperclass c)))))

;;Here's the source from clojure/src/clj/core_deftype.clj
(defn implements? [protocol atype]
  (and atype (.isAssignableFrom ^Class (:on-interface protocol) atype)))

(defn my-extends? 
  "Returns true if atype extends protocol"
  {:added "1.2"}
  [protocol atype]
  (boolean (or (implements? protocol atype) 
               (get (:impls protocol) atype))))

;; I completely missed the fact that clojure.lang.IKVReduce popped up during
;; the refactor, and is used as a fast-path for classes that implement it
;; during kv-reduce.  

;; I chased my tail trying to figure out why maps don't extend
;; clojure.core.protocols/IKVReduce, which is simply because the interface
;; clojure.core.protocols.IKVReduce is no longer implemented, leaving
;; isAssignAbleFrom reflection call to return false now, while providing a
;; protocol implementation for clojure.lang.IKVReduce.  The protocol
;; implementation delegates to the class's implementation of
;; clojure.lang.IKVReduce if possible.

;; So, satisfies? traces through superclasses trying to find a protocol
;; implementation, which is far more durable (depending on the protocol)
;; rather than class-specific interface implementations I was using to
;; cheat out a fastpath.  

;; Just for grins, here's the microbenchmark associated with the "fast path"
;; vs cached method lookup:

(def the-map {:a 2})
 
;;note the clojure.lang.IKVReduce
(defn ikv? [x] (instance? clojure.lang.IKVReduce x))

(let [cache (java.util.HashMap.)]
  (defn sat? [protocol x]
    (let [c (class x)]
      (if-let [res (.get cache c)] ;we know.
        res
        (let [res (satisfies? protocol x)
              _ (.put cache c res)]
          res)))))
  
  
;;Microbenchmarks.

;;(time (dotimes [i 1000000] (sat? clojure.core.protocols/IKVReduce the-map)))
;;"Elapsed time: 13.31948 msecs"

;;(time (dotimes [i 1000000] (ikv? the-map))
;;"Elapsed time: 6.816777 msecs"

;Negligible cost for caching (preferably using a concurrent map in production).

