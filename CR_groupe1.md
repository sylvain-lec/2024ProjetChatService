# Compte rendu - groupe 1
**dépôt git** : https://github.com/sylvain-lec/2024ProjetChatService

## Liste des fonctionnalités

**Faire une interface graphique**  
Swing: https://docs.oracle.com/javase/tutorial/uiswing/index.html  
FlatLaf: https://github.com/JFormDesigner/FlatLaf

**Constitution d'un carnet d'adresses**
  - Ajout et suppression de contacts
  - Affichage de la liste des contacts
  - Rechercher un contact par son nom/id
  - Rendre la liste privée (visible seulement par l'utilisateur concerné)

**Identification**
- Ajout de noms associés aux clients (utilisateurs ou groupes)
- Ajout d'un Avatar
- Connexion sécurisée/authentification : ajout d'un mot de passe
- Afficher le statut de connexion des utilisateurs

**Envoi de différents contenus**  
  - Permettre l'envoi d'images. Il faudra modifier Packet.java (et méthode sendPacket dans ClientMsg.java)? pour inclure le type de média et la taille
  - Lecture des emojis   

**Envoi de différents types de paquets**   
  - Modification des noms des utilisateurs et groupes
  - Gestion des groupes
    - Suppression/ajout de membres dans les groupes
    - Création/suppression d'un groupe
    - Notification pour tous les membres du groupes lors d'une modification
  - Modification d'un message existant

**BDD**  
Etude du fonctionnement de la DB Derby : https://db.apache.org/derby/  
Ajout des diverses fonctionnalités dans la DB (carnet d'adresse, noms, historique des échanges, ...)
  

## Fonctionnalités optionnelles  

- 
-

## Protocoles

-
-

### Répartition des tâches  

  
Pour la fin de la journée, vous devez déposer un document au format libre avec la liste des 
fonctionalités que vous engagez à réaliser, et celles optionelles. Vous devez avoir spécifié 
le protocole, i.e. les formats de messages que vous avez défini pour implémenter les 
fonctionnalités. Par exemple, paquet de création de groupe commence par la valeur 1
(byte indiquant le type de message serveur), puis nombre de membres (int), puis liste des
identifiants des mmembres (ints)). Vous devez aussi donner le lien vers votre dépot git, 
et expliquer un peu votre politique de gestion de projet (comment et où sont répertoiriées 
les taches et leur affectation), etc.