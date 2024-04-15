# Compte rendu - groupe 1
**dépôt git** : https://github.com/sylvain-lec/2024ProjetChatService

## Liste des fonctionnalités

### Faire une interface graphique
Swing: https://docs.oracle.com/javase/tutorial/uiswing/index.html  
FlatLaf: https://github.com/JFormDesigner/FlatLaf

### Constitution d'un carnet d'adresses
- Création d'une table dans la bdd
- Ajout et suppression de contacts 
- Affichage de la liste des contacts
- Rechercher un contact par son nom/id
- Rendre la liste privée (visible seulement par l'utilisateur concerné)

### Identification
- Ajout d'un attribut nom aux clients (utilisateurs ou groupes)
- Ajout d'un Avatar
- Connexion sécurisée/authentification : ajout d'un mot de passe
- Afficher le statut de connexion des utilisateurs

### pour les protocoles
On peut créer une classe qui analyse le paquet en f° des protocoles définis?  


### Paquets messages
- Permettre l'envoi d'images. Il faudra modifier Packet.java (et méthode sendPacket
dans ClientMsg.java) pour inclure le type de média
- protocole : premier int pr le destinataire, deuxième int pour le type de média, 
troisième int pour la longueur de la data, le reste pour le contenu
- Lecture des emojis   
- Modification d'un message. Garder le timestamp des messages et les stocker dans la bdd  

### Autres types de paquets

**Modification des noms des utilisateurs et groupes**  
    - **protocole** : premier int à 5, deuxième int avec l'id du groupe/du user, troisième int avec la longueur des données,
puis les données (i.e le nouveau nom)
    - ajouter une méthode editName() dans ServerPacketProcessor qui appelle une nouvelle méthode
editName() dans ServerMsg

**Gestion des groupes**

- Ajout de membres dans les groupes 
    - **protocole** : premier int à 3, deuxième int avec l'id du groupe, troisième int avec l'id de l'utilisateur 
    - ajout d'une méthode addMember() dans ServerPacketProcessor  
    - L'utilisateur invité doit recevoir un message d'invitation


- Suppression de membres dans les groupes
    - **protocole** : premier int à 4, deuxième int avec l'id du groupe, troisième int avec l'id de l'utilisateur
    - ajouter d'une méthode removeMember() dans ServerPacketProcessor.java
    - envoi d'un message à tous les membres restants du groupe


- Suppression d'un groupe
  - **protocole** : premier int à 2, deuxième int pour l'id du groupe
  - créer une méthode removeGroup() dans ServerPacketProcessor.java, qui utilise la méthode removeGroup() de ServerMsg.java


- Modification d'un message existant

### BDD
Etude du fonctionnement de la DB Derby : https://db.apache.org/derby/  
Ajout des diverses fonctionnalités dans la DB (carnet d'adresse, noms, historique des échanges, ...)
  
### Tests fonctionnels  
- Ecriture de scénario de tests
- Exécution (manuellement si plus simple)
- Reporter les bug dans un backlog

### Documentation  
Ecriture de la documentation du projet

## Fonctionnalités optionnelles  
Indicateur de réception des messages pour l'expéditeur  
Chiffrement des échanges  
Intégration de messages vocaux  
Recherche dans les messages (choisir la bonne structure de données)  


### Répartition des tâches  
**Mardi** : groupe complet sur ??  
**Jours suivants** : Binômes

### Trello :  
Les binômes se positionnent sur les cartes (add members)   
Ils peuvent ajouter les autres si besoin (pour des tests ou debug par exemple)  
**Les tâches circulent d'un état à un autre**: Backlog, To Do, In Progress, Testing, Review, Done  
**Utilisation de labels pour un repère visuel des types d'éléments** : feature, enhancement, critical, bug