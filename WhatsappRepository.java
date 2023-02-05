package com.driver;

import java.time.LocalDate;
import java.util.*;

import org.springframework.stereotype.Repository;

@Repository
public class WhatsappRepository {


    private HashMap<Group, List<User>> groupUserMap;//group db
    private HashMap<Group, List<Message>> groupMessageMap;//message db
    private HashMap<Message, User> senderMap;//sender db
    private HashMap<Group, User> adminMap;// admin db
    private HashSet<String> userMobile;// user db
    private int customGroupCount;
    private int messageId;

    public WhatsappRepository(){
        this.groupMessageMap = new HashMap<Group, List<Message>>();
        this.groupUserMap = new HashMap<Group, List<User>>();
        this.senderMap = new HashMap<Message, User>();
        this.adminMap = new HashMap<Group, User>();
        this.userMobile = new HashSet<>();
        this.customGroupCount = 0;
        this.messageId = 0;
    }
    public String createUser(String name, String mobile) throws Exception {

        if(userMobile.contains(mobile)){
            throw new Exception("User already exists");
        }
        userMobile.add(mobile);//it is a hashset so we added only mobile
        User user = new User(name, mobile);//to create new user with user object
        return "SUCCESS";
    }
    public Group createGroup(List<User> users){

        if(users.size()==2){//for 2 members
            //grop is <Group, list<user>> list is provided
            Group group = new Group(users.get(1).getName(), 2);//so creating new group with its object
            adminMap.put(group, users.get(0));//storing the data of admin db
            groupUserMap.put(group, users);//putting in group db
            groupMessageMap.put(group, new ArrayList<Message>());//creating new msg data base
            return group;
        }
        this.customGroupCount += 1;//count of custom group
        Group group = new Group(new String("Group "+this.customGroupCount), users.size());
        adminMap.put(group, users.get(0));
        groupUserMap.put(group, users);
        groupMessageMap.put(group, new ArrayList<Message>());
        return group;
    }
    public int createMessage(String content){
        // The 'i^th' created message has message id 'i'.
        // Return the message id.
        this.messageId += 1;
        Message message = new Message(messageId, content);
        return message.getId();
    }
    public int sendMessage(Message message, User sender, Group group) throws Exception{

        if(adminMap.containsKey(group)){//checking the group is present or not
            List<User> users = groupUserMap.get(group);//iterating the users in group DB
            Boolean userFound = false;
            for(User user: users){//iterating through user list
                if(user.equals(sender)){
                    userFound = true;
                    break;
                }
            }
            if(userFound){
                senderMap.put(message, sender);//sending the msg
                List<Message> messages = groupMessageMap.get(group);//HashMap<Group, List<Message>> groupMessageMap
                messages.add(message);//storing in the message DB
                groupMessageMap.put(group, messages);
                return messages.size();
            }
            throw new Exception("You are not allowed to send message");//if user not fount
        }
        throw new Exception("Group does not exist");//if group doesn't exist
    }
    public String changeAdmin(User approver, User user, Group group) throws Exception{


        if(adminMap.containsKey(group)){
            if(adminMap.get(group).equals(approver)){
                List<User> participants = groupUserMap.get(group);
                Boolean userFound = false;
                for(User participant: participants){
                    if(participant.equals(user)){
                        userFound = true;
                        break;
                    }
                }
                if(userFound){
                    adminMap.put(group, user);
                    return "SUCCESS";
                }
                throw new Exception("User is not a participant");
            }
            throw new Exception("Approver does not have rights");
        }
        throw new Exception("Group does not exist");
    }
    public int removeUser(User user) throws Exception{

        Boolean userFound = false;
        Group userGroup = null;
        for(Group group: groupUserMap.keySet()){
            List<User> participants = groupUserMap.get(group);
            for(User participant: participants){
                if(participant.equals(user)){
                    if(adminMap.get(group).equals(user)){
                        throw new Exception("Cannot remove admin");
                    }
                    userGroup = group;
                    userFound = true;
                    break;
                }
            }
            if(userFound){
                break;
            }
        }
        if(userFound){
            List<User> users = groupUserMap.get(userGroup);
            List<User> updatedUsers = new ArrayList<>();
            for(User participant: users){
                if(participant.equals(user))
                    continue;
                updatedUsers.add(participant);
            }
            groupUserMap.put(userGroup, updatedUsers);

            List<Message> messages = groupMessageMap.get(userGroup);
            List<Message> updatedMessages = new ArrayList<>();
            for(Message message: messages){
                if(senderMap.get(message).equals(user))
                    continue;
                updatedMessages.add(message);
            }
            groupMessageMap.put(userGroup, updatedMessages);

            HashMap<Message, User> updatedSenderMap = new HashMap<>();
            for(Message message: senderMap.keySet()){
                if(senderMap.get(message).equals(user))
                    continue;
                updatedSenderMap.put(message, senderMap.get(message));
            }
            senderMap = updatedSenderMap;
            return updatedUsers.size()+updatedMessages.size()+updatedSenderMap.size();
        }
        throw new Exception("User not found");
    }

    public String findMessage(Date start, Date end, int K) throws Exception{

        List<Message> messages = new ArrayList<>();
        for(Group group: groupMessageMap.keySet()){
            messages.addAll(groupMessageMap.get(group));
        }
        List<Message> filteredMessages = new ArrayList<>();
        for(Message message: messages){
            if(message.getTimestamp().after(start) && message.getTimestamp().before(end)){
                filteredMessages.add(message);
            }
        }
        if(filteredMessages.size() < K){
            throw new Exception("K is greater than the number of messages");
        }
        Collections.sort(filteredMessages, new Comparator<Message>(){
            public int compare(Message m1, Message m2){
                return m2.getTimestamp().compareTo(m1.getTimestamp());
            }
        });
        return filteredMessages.get(K-1).getContent();
    }
}
