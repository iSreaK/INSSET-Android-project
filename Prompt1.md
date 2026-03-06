Tu vas reprendre la base de projet Android que tu viens de générer.

IMPORTANT
Tu ne dois PAS recréer un projet from scratch.
Tu dois PARTIR de l’architecture existante déjà générée, la relire, la corriger si besoin, puis l’ADAPTER au vrai sujet du projet.

CONTEXTE
Le projet a changé :
- le langage est Java
- le nom du projet est : jv-Bench

Le thème de l’application est le suivant :

jv-Bench est une application Android collaborative permettant de géolocaliser des bancs publics sur une carte.
Les utilisateurs peuvent :
- visualiser les bancs sur une carte
- ajouter un banc à un endroit précis
- associer à un banc :
  - une image
  - un nom
  - une description
- consulter le détail d’un banc
- ajouter un avis sur un banc
- noter un banc sur 10
- laisser un commentaire

L’objectif du projet est de fournir une base propre pour un travail en équipe à 4 personnes.
Je veux donc que tu adaptes l’architecture à ce sujet, SANS implémenter les fonctionnalités métier complètes pour l’instant.

CONTRAINTE ABSOLUE
Tu ne dois PAS développer les features complètes maintenant.
Tu dois uniquement :
- adapter le squelette
- renommer les concepts
- ajuster les modèles
- ajuster les repositories/interfaces
- ajuster les fragments/viewmodels/layouts de base
- préparer des TODO clairs pour que les 4 membres puissent ensuite développer leurs parties

STACK À RESPECTER
- Langage : Java
- UI : XML
- Architecture : MVVM simple
- 1 seule Activity
- plusieurs Fragments
- Navigation Component
- OpenStreetMap
- Supabase
- injection manuelle simple
- async simple compatible Java
- code lisible par des étudiants
- pas d’architecture complexe
- pas de Hilt
- pas de Compose
- pas de multi-module

OBJECTIF DE CETTE PASSE
Adapter l’architecture existante au projet jv-Bench pour qu’elle soit cohérente, claire, prête pour le travail en équipe, et sans implémenter les fonctionnalités finales.

CE QUE TU DOIS FAIRE

1. RELIRE ET CORRIGER LA BASE EXISTANTE
- analyse l’architecture déjà générée
- corrige les incohérences éventuelles
- harmonise les noms de packages, classes, fragments, layouts, viewmodels et repositories
- remplace les concepts génériques ou anciens concepts du projet précédent par ceux du projet jv-Bench
- assure la cohérence Java + Android XML + Navigation Component + MVVM

2. ADAPTER LE DOMAINE MÉTIER AU PROJET jv-Bench
Le concept central n’est plus “Event” mais “Bench”.

Je veux que tu adaptes l’architecture autour des entités suivantes :

A. Bench
Champs souhaités :
- id : String
- name : String
- description : String
- latitude : double
- longitude : double
- imageUrl : String
- authorId : String
- createdAt : long
- averageRating : double
- reviewCount : int

B. Review
Champs souhaités :
- id : String
- benchId : String
- userId : String
- rating : int
- comment : String
- createdAt : long

C. User
Champs souhaités :
- id : String
- email : String
- role : UserRole

D. UserRole
Valeurs :
- USER
- ADMIN

E. GeoPoint
Champs :
- latitude : double
- longitude : double

IMPORTANT :
- crée ces modèles en Java
- simples, propres, lisibles
- pas de logique complexe dedans
- getters/setters/constructeurs si nécessaire
- utilise un style cohérent dans tout le projet

3. ADAPTER LES CONTRATS DE REPOSITORY
Je veux une base claire avec des interfaces, sans implémentation ultra avancée.

Créer ou adapter au minimum :

AuthRepository
- signIn(String email, String password, AuthCallback<User> callback)
- signUp(String email, String password, AuthCallback<User> callback)
- signOut(ResultCallback<Void> callback)
- getCurrentUser()

BenchRepository
- getBenches(ResultCallback<List<Bench>> callback)
- getBenchById(String id, ResultCallback<Bench> callback)
- createBench(Bench bench, ResultCallback<Void> callback)

ReviewRepository
- getReviewsForBench(String benchId, ResultCallback<List<Review>> callback)
- addReview(Review review, ResultCallback<Void> callback)

IMPORTANT :
- comme on est en Java, utilise une approche simple et cohérente pour l’asynchrone
- évite toute solution compliquée
- tu peux créer de petites interfaces callback simples de type ResultCallback<T>
- le but est d’avoir une base claire et extensible
- ne code pas toute la logique Supabase finale maintenant

4. ADAPTER LA STRUCTURE UI
Je veux que les écrans de base correspondent au projet jv-Bench.

Fragments à prévoir :
- LoginFragment
- RegisterFragment
- MapFragment
- BenchFormFragment
- BenchDetailFragment
- ReviewFormFragment (optionnel si tu juges utile dès maintenant, sinon prépare simplement le terrain)

IMPORTANT :
- tu ne dois pas coder les vraies features métier
- tu dois juste créer les écrans de base, la navigation minimale, les classes, les layouts XML, et des TODO clairs

5. ADAPTER LES VIEWMODELS
Je veux des ViewModels Java simples, cohérents avec MVVM, pour les écrans principaux.

Créer/adapter :
- LoginViewModel
- RegisterViewModel
- MapViewModel
- BenchFormViewModel
- BenchDetailViewModel
- éventuellement ReviewFormViewModel si tu crées ce fragment

IMPORTANT :
- pas de logique avancée
- juste la structure
- états minimaux
- LiveData si c’est plus simple en Java
- code pédagogique et lisible

6. ADAPTER LA NAVIGATION
Je veux que la navigation reflète jv-Bench.

Flux minimal attendu :
- MainActivity héberge le NavHostFragment
- Login -> Register
- Login -> Map
- Map -> BenchForm
- Map -> BenchDetail
- BenchDetail -> éventuellement ReviewForm si tu le crées

IMPORTANT :
- navigation en place
- destinations cohérentes
- IDs de navigation et layouts harmonisés

7. OPENSTREETMAP / MAP
Je veux conserver une base OpenStreetMap.
Dans MapFragment :
- garde une carte de base fonctionnelle si elle existe déjà
- sinon ajoute l’intégration minimale nécessaire
- centrage par défaut sur une position exemple
- ajoute uniquement des placeholders / TODO pour :
  - affichage des bancs
  - ajout futur des marqueurs
  - interaction avec un banc
- ne développe pas la feature complète d’affichage dynamique

8. SUPABASE
Je veux conserver une base d’intégration Supabase, mais uniquement au niveau squelette.
- garde un provider/client centralisé
- adapte les repositories pour Bench / Review / Auth
- prépare les zones TODO pour :
  - auth
  - benches
  - reviews
  - image upload
- ne hardcode aucun secret
- garde les placeholders explicites
- prépare proprement la structure des fichiers

9. ARBORESCENCE CIBLE
Adapte l’arborescence existante pour qu’elle ressemble à quelque chose comme :

com.example.jvbench
├── di
│   ├── App.java
│   └── AppContainer.java
├── core
│   ├── common
│   │   ├── ResultCallback.java
│   │   └── AuthCallback.java
│   ├── location
│   ├── permissions
│   └── navigation
├── domain
│   ├── model
│   │   ├── Bench.java
│   │   ├── Review.java
│   │   ├── User.java
│   │   ├── UserRole.java
│   │   └── GeoPoint.java
│   └── repository
│       ├── AuthRepository.java
│       ├── BenchRepository.java
│       └── ReviewRepository.java
├── data
│   ├── remote
│   │   └── supabase
│   ├── repository
│   └── mapper
└── ui
    ├── main
    ├── auth
    ├── map
    ├── benchform
    ├── benchdetail
    └── reviewform

Tu peux ajuster légèrement, mais garde cette logique.

10. NOMMAGE / IDENTITÉ DU PROJET
Adapte tout ce qui doit l’être pour que le projet s’appelle correctement :
- jv-Bench
- package cohérent, par exemple `com.example.jvbench` ou autre package simple cohérent
- noms de classes et ressources cohérents avec le thème “bench”
- textes minimums dans strings.xml adaptés au projet

11. README
Je veux que tu mettes à jour le README pour expliquer clairement :

- le nom du projet : jv-Bench
- le concept de l’application
- la stack technique
- la structure du projet
- ce qui est déjà prêt dans l’architecture
- ce qui reste volontairement en TODO
- les grandes zones futures de développement pour une équipe de 4

IMPORTANT :
Le README doit expliquer que cette base est volontairement un squelette et que les fonctionnalités seront développées ensuite par les membres du groupe.

12. GITIGNORE / PROPRETÉ
Vérifie que le .gitignore est adapté à Android / Java.
Ne rajoute pas d’outillage inutile.

13. CE QUE TU NE DOIS PAS FAIRE
Ne fais PAS :
- implémentation complète des features
- logique Supabase finale complète
- système complet de notes/commentaires
- upload d’image complet
- logique de géolocalisation avancée
- architecture enterprise
- sur-abstraction
- tests avancés
- CI/CD
- documentation excessive

14. CE QUE JE VEUX À LA FIN
Je veux un résultat qui soit :
- cohérent avec le projet jv-Bench
- prêt pour être poussé sur GitHub
- compréhensible par 4 étudiants
- simple à reprendre pour développer les features chacun de notre côté
- sans logique métier terminée, mais avec une base solide

15. FORMAT DE TA RÉPONSE
Travaille directement sur les fichiers existants si tu peux.
Sinon :
- montre l’arborescence finale mise à jour
- puis liste les fichiers modifiés / créés
- puis donne le contenu des principaux fichiers adaptés
- puis termine par un résumé clair des TODO laissés volontairement pour l’équipe

PRIORITÉ ABSOLUE
1. Adapter proprement l’architecture existante au vrai sujet
2. Rester simple et cohérent
3. Préparer le travail collaboratif
4. Ne PAS développer les features finales