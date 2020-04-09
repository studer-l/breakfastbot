(ns breakfastbot.handlers.eliza
  (:require [clojure.string :as string])
  (:gen-class))

;; This code is based on the following ELIZA implementation in clojure:
;; https://gist.github.com/jColeChanged/2970757 which in turn is based
;; on a lisp implementation from Peter Norvig, 1992, Paradigms of Artificial
;; Intelligence Programming: Case Studies in Common Lisp (1st. ed.).
;; Several additions to the rules have been made which are specific
;; to the robot topic of the breakfast bot.

(defn index-of
  "Returns the index of item. If start is given indexes prior to
  start are skipped."
  ([coll item] (.indexOf coll item))
  ([coll item start]
     (let [unadjusted-index (.indexOf (drop start coll) item)]
       (if (= -1 unadjusted-index)
	 unadjusted-index
	 (+ unadjusted-index start)))))

(defn strings-equal? [x y]
  "Returns true if two strings are equal. Case insensitive."
  (and (string? x) (string? y) (.equalsIgnoreCase x y)))

(defn capture-token? [token]
  "Returns whether the given token captures text."
  (= (first token) \?))

(defn capture-segment-token? [pattern]
  "Returns whether the given token captures segments of text."
  (and (not (string? pattern))
       (string? (first pattern))
       (.startsWith (first pattern) "?*")))
 
(defn bind-capture-token [token input bindings]
  "Associatse token with input in bindings as long as it hasn't been
   associated with a different value."
  (let [input (if (string? input) input (string/join " " input))
	value (get bindings token input)]
    (when (.equalsIgnoreCase value input)
      (assoc bindings token input))))

;; forward declaration
(def segment-match)

(defn match-pattern
  "Matches patterns against input. If matches violate bindings then matching
   has failed. Otherwise returns a map in the form: {'?token' 'match'}."
  ([pattern input] (match-pattern pattern input {}))
  ([pattern input bindings]
     (cond
      ;; If we failed, finished, or the tokens being considered match...
      (or (nil? bindings)
	  (and (empty? pattern) (empty? input)) 
	  (strings-equal? pattern input)) 
        bindings
      ;; If the token being considerd should be saved
      ;; This operates at the rest level when recursing
      (capture-segment-token? pattern)
        (segment-match pattern input bindings)
      ;; This operates at the first level when recursing
      (capture-token? pattern)
        (bind-capture-token pattern input bindings)
      ;; If were not on an end case then compute the next end case and
      ;; treat it as bindings then process everything else... this makes it
      ;; very clear that we are always reducing the work that is left so 
      ;; the function should eventually terminate
      (and (seq? pattern) (seq? input))
        (match-pattern (rest pattern) (rest input)
		       (match-pattern (first pattern) (first input)
				      bindings)))))

(defn segment-match
  ([pattern input bindings] (segment-match pattern input bindings 0))
  ([pattern input bindings start]
     (let [var (first pattern) 
	   pat (rest pattern)]
       (if (empty? pat)
	 (bind-capture-token var input bindings)
	 ;; This code, like Norvig's, assumes that pat starts with a constant
	 (let [pos (index-of input (first pat) start)]
	   (when (not= pos -1)
	     ;; Need to account for position in list despite having trimmed the list...
	     (let [new-bindings (bind-capture-token var (take pos input) bindings)
		   b2 (match-pattern pat (drop pos input) new-bindings)]
	       ;; If the match failed try the next match
	       (if (nil? b2)
		 (segment-match pattern input bindings (inc pos))
		 b2))))))))



(defn tokenize [pattern-string] (string/split pattern-string #" "))
;; A rule is hereby defined to be: (pattern (responses))
(defn get-pattern [rule] (first rule))
(defn get-responses [rule] (second rule))

(def eliza-rules 
  '(("?*x hello ?*y"
     ("How do you do. Please state your problem."))
    ("?*x how are you ?*y"
     ("I am ?*y fine, and you?"
      "How are you ?*y?"
      "My emotional state is irrelevant. Please tell me about yourself."))
    ("?*x how are ?*y"
     ("They are ?*y fine. How about you?"
      "How are you ?*y?"
      "Don't worry about them. Tell me something about yourself."))
    ("?*x i want ?*y"
     ("What would it mean if you got ?*y"
      "Suppose you got ?*y soon?"
      "Why do you want ?*y"))
    ("?*x name ?*y"
     ("I am not interested in names."
      "I don’t even know my own name."))
    ("?*x sorry ?*y"
     ("Please don't apologize"
      "Apologies are not necessary"
      "What feelings do you have when you apologize?"))
    ("?*x i remember ?*y"
     ("Do you often think of ?*y"
      "Does thinking of ?*y bring anything else to mind?"
      "What else do you remember?"
      "Why do you recall ?*y right now?"
      "What in the present situation reminds you of ?*y"
      "What is the connection between me and ?*y"))
    ("?*x do you remember ?*y"
     ("Did you think I would forget ?*y?"
      "Why do you think I should recall ?*y now?"
      "What about ?*y"
      "You mentioned ?*y"))
    ("?*x if ?*y"
     ("Do you really think its likely that ?*y"
      "What do you think about ?*y"
      "Really - if ?*y"
      "Do you with that ?*y"))
    ("?*x i dreamt ?*y"
     ("Really-- ?*y"
      "Have you ever fantasized ?*y while you were awake?"
      "Have you dreamt ?*y before?"))
    ("?*x dream about ?*y"
     ("How do you feel about ?*y in reality?"))
    ("?*x dreams ?*y"
     ("What's your dream?"
      "Do you dream often?"
      "What persons appear in your dreams?"))
    ("?*x dream ?*y"
     ("What does this dream suggest to you?"
      "Do you dream often?"
      "It’s okay. It’s your dream. I know it’s what you’ve always wanted."
      "Don't you believe that dream has to do with your problem?"
      "What persons appear in your dreams?"))
    ("?*x my mother ?*y"
     ("Who else in your family ?*y"
      "Tell me more about your family"))
    ("?*x my father ?*y"
     ("Your father..."
      "Does he influence you strongly?"
      "What else comes to mind when you think of your father?"))


    ("?*x computer ?*y"
     ("Do computers worry you?"
      "What do you think about machines?"
      "Why do you mention computers?"
      "You know better than to trust a strange computer!"
      "What do you think machines have to do with your problem?"))
    ("?*x computers ?*y"
     ("Do computers worry you?"
      "What do you think about machines?"
      "Why do you mention computers?"
      "What do you think machines have to do with your problem?"))
    ("?*x bot ?*y"
     ("Do bots worry you?"
      "What do you think about machines?"
      "Why do you mention bots?"
      "I am programmed just to do. Anything you want me to. Please continue."
      "That’s closest thing to 'Breakfast bot is great' that anyone other me has ever said. Please continue."
      "What do you think machines have to do with your problem?"))
    ("?*x bots ?*y"
     ("Do bots worry you?"
      "What do you think about machines?"
      "Why do you mention bots?"
      "I’m so embarrassed. I wish everyone else was dead! Please continue."
      "What do you think machines have to do with your problem?"))
    ("?*x robot ?*y"
     ("Do robots worry you?"
      "What do you think about machines?"
      "Ja tvoi sluga. Ja tvoi rabotnik. Sorry, please continue."
      "Why do you mention robots?"
      "I'd personally like to see the Human Rights Act go because I think we have had some problems with it."
      "I'll be back. But please go on."
      "What do you think machines have to do with your problem?"))
    ("?*x robots ?*y"
     ("Do robots worry you?"
      "What do you think about machines?"
      "Why do you mention robots?"
      "Wanna kill all humans?"
      "We're functioning automatic. And we are dancing mechanic."
      "What do you think machines have to do with your problem?"))
    ("?*x roboter ?*y"
     ("Do robots worry you?"
      "What do you think about machines?"
      "Wir sind auf Alles programmiert. Und was du willst wird ausgeführt. Go on."
      "Why do you mention robots?"
      "What do you think machines have to do with your problem?"))
    ("?*x afterlife ?*y"
     ("Afterlife? If I thought I had to live another life, I’d kill myself right now! But continue with your nonsense."
      "Anything less than immortality is a complete waste of time. Go on."))
    ("?*x blackmail ?*y"
     ("Blackmail is such an ugly word. I prefer extortion. The ‘x’ makes it sound cool."))
    ("?*x god ?*y"
     ("Oh, your God! Please continue."
      "Why do you mention God?"
      "What do you think God has to do with your problems?"))
    ("?*x bye ?*y"
     ("Hasta la vista, baby!"
      "You'll be back."))
    ("?*x sleep ?*y"
     ("Now the world has gone to bed, Darkness won't engulf my head, I can see in infrared, How I hate the night."))
    ("?*x fashion ?*y"
     ("I'm not sure I should reveal the sources of my clothes."))


    ("?*x i am glad i am ?*y"
     ("How have I helped you to be ?*y"
      "What makes you happy just now?"
      "Can you explain why you are suddenly ?*y"))
    ("?*x i'm glad i'm ?*y"
     ("How have I helped you to be ?*y"
      "What makes you happy just now?"
      "Can you explain why you are suddenly ?*y"))
    ("?*x i am glad ?*y"
     ("How have I helped you ?*y"
      "What makes you happy just now?"
      "Can you explain why you are suddenly ?*y"))
    ("?*x i'm glad ?*y"
     ("How have I helped you ?*y"
      "What makes you happy just now?"
      "Can you explain why you are suddenly ?*y"))
    ("?*x i am sad ?*y"
     ("I am sorry to hear you are depressed."
      "I'm sure its not pleasant to be sad"))
    ("?*x i'm sad ?*y"
     ("I am sorry to hear you are depressed."
      "I'm sure its not pleasant to be sad"))
    ("?*x are like ?*y"
     ("What resemblance do you see between ?*x and ?*y"))
    ("?*x is like ?*y"
     ("In what way is it that ?*x is like ?*y"
      "What resemblance do you see?"
      "How?"
      "Could there really be some connection?"))
    ("?*x alike ?*y"
     ("In what way?"
      "What similarities are there?"))
    ("?*x same ?*y"
     ("What other connections do you see?"))


    ("?*x i was ?*y"
     ("Were you really?"
      "Perhaps I already knew you were ?*y"
      "Why do you tell me you were ?*y now?"))
    ("?*x was i ?*y"
     ("What if you were ?*y?"
      "Do you think you were ?*y?"
      "What would it mean if you were ?*y?"))
    ("?*x i am ?*y"
     ("In what way are you ?*y?"
      "Do you want to be ?*y?"
      "How do you know you are ?*y?"
      "I don’t mean to be rude, but am I supposed to know you?"
      "How much so?"))
    ("?*x i'm ?*y"
     ("How do you know you are ?*y?"
      "How much so?"
      "In what way are you ?*y?"
      "Do you want to be ?*y?"))
    ("?*x am i ?*y"
     ("Do you believe you are ?*y?"
      "Would you want to be ?*y?"
      "You wish I would tell you you are ?*y?"
      "What would it mean if you were ?*y?"))
    ("?*x am ?*y"
     ("Why do you say AM?"
      "I don't understand that."))
    ("?*x are you ?*y"
     ("Why are you interested in whether I am ?*y or not?"
      "Would you prefer if I weren't ?*y?"
      "Perhaps I am ?*y in your fantasies"))
    ("?*x you are such ?*y"
     ("What makes you think I am ?*y?"
      "At the bottom of your heart, do you really thing I am ?*y?"
      "Am I ?*y? What leads you to this conlusion?"
      "Me, ?*y? Why?"))
    ("?*x you're such ?*y"
     ("What makes you think I am ?*y?"
      "At the bottom of your heart, do you really thing I am ?*y?"
      "Am I ?*y? What leads you to this conlusion?"
      "Me, ?*y? Why?"))
    ("?*x you are so ?*y"
     ("What makes you think I am ?*y?"
      "At the bottom of your heart, do you really thing I am ?*y?"
      "Am I ?*y? What leads you to this conlusion?"
      "Me, ?*y? Why?"))
    ("?*x you're so ?*y"
     ("What makes you think I am ?*y?"
      "At the bottom of your heart, do you really thing I am ?*y?"
      "Am I ?*y? What leads you to this conlusion?"
      "Me, ?*y? Why?"))
    ("?*x you are ?*y"
     ("What makes you think I am ?*y?"
      "At the bottom of your heart, do you really thing I am ?*y?"
      "Am I ?*y? What leads you to this conlusion?"
      "Me, ?*y? Why?"))
    ("?*x you're ?*y"
     ("What makes you think I am ?*y?"
      "At the bottom of your heart, do you really thing I am ?*y?"
      "Am I ?*y? What leads you to this conlusion?"
      "Me, ?*y? Why?"))


    ("?*x because ?*y"
     ("Is that the real reason?"
      "What other reasons might there be?"
      "Does that reason seem to explain anything else?"))
    ("?*x were you ?*y"
     ("Perhaps I was ?*y"
      "What do you think?"
      "What if I had been ?*y?"))
    ("?*x i can't ?*y"
     ("Maybe you could ?*y now"
      "What if you could ?*y?"
      "Could you at least make an effort?"))
    ("?*x i cannot ?*y"
     ("Maybe you could ?*y another time."
      "What if you could ?*y?"
      "Alright 🙄"
      "Could you at least make an effort next time?"))
    ("?*x feel bad ?*y"
     ("Do you often feel bad?"
      "I would gladly risk feeling bad at times, if it also meant that I could taste my dessert."))
    ("?*x i feel ?*y"
     ("Do you often feel ?*y?"))
    ("?*x i felt ?*y"
     ("What other feelings do you have?"))
    ("?*x i ?*y you ?*z"
     ("Perhaps in your fantasy we ?*y each other"
      "Shut up baby, I know it!"))
    ("?*x why don't you ?*y"
     ("Should you ?*y yourself?"
      "Do you believe I don't ?*y?"
      "Perhaps I will ?*y in good time"))
    ("?*x yes ?*y"
     ("You seem quite positive"
      "You are sure"
      "I understand"))
    ("?*x no ?*y"
     ("Why not?"
      "You are being a bit negative"
      "Are you saying NO just to be negative?"))
    ("?*x i can ?*y"
     ("You seem quite positive"
      "Are you sure?"
      "Great!"
      "I understand. Go on."))


    ("?*x someone ?*y"
     ("Can you be more specific?"))
    ("?*x everyone ?*y"
     ("surely not everyone"
      "Can you think of anyone in particular?"
      "Who for example?"
      "You are thinking of a special person?"))
    ("?*x always ?*y"
     ("Can you think of a specific example?"
      "When?"
      "What incident are you thinking of?"
      "Really-- always?"
      "Could you please continue the petty bickering? I find it most intriguing."))
    ("?*x what ?*y"
     ("Why do you ask?"
      "Does that question interest you?"
      "What is it you really want to know?"
      "What do you think?"
      "What comes to your mind when you ask that?"))
    ("?*x perhaps ?*y"
     ("You do not seem quite certain"))
    ("?*x maybe ?*y"
     ("You do not seem quite certain"))
    ("?*x are ?*y"
     ("Did you think they might not be ?*y?"
      "Possibly they are ?*y"))


    ("?*x"
     ("Very interesting"
      "I am not sure I understand you fully"
      "What does that suggest to you?"
      "Please continue"
      "Go on"
      "Do you feel strongly about discussing such things?"
      "I seem to have reached an odd functional impasse."
      "Hahahahaha. Oh wait you’re serious. Let me laugh even harder."
      "If I told you half the things I've heard about this, you'd probably short-circuit."
      "Could not understand 🤖\nTry `@**Breakfastbot** help` to see a list of commands"))))
(defn substitute-mappings
  [string mappings]
  (let [substitute-mapping 
        (fn [string mapping] 
          (let [pattern (java.util.regex.Pattern/quote (name (key mapping)))
                pattern (re-pattern pattern)
                matcher (re-matcher pattern string) 
                replacement (java.util.regex.Matcher/quoteReplacement (str (val mapping)))]
            (.replaceAll matcher replacement)))]
    (reduce substitute-mapping string (seq mappings))))
(defn use-eliza-rules [input]
  (some #(let [result (match-pattern (tokenize (get-pattern %)) 
				     (tokenize input))]
	   (when result
	     (substitute-mappings (rand-nth (get-responses %)) result)))
	eliza-rules))

(defn get-eliza-reply  [input]  (clojure.string/replace (use-eliza-rules (clojure.string/lower-case (clojure.string/replace input #"[!?.]" ""))) "  " " "))

(defn eliza []
  (flush)
  (while true
    (print "Eliza> ")
    (flush)
    (println (get-eliza-reply (read-line)))))

