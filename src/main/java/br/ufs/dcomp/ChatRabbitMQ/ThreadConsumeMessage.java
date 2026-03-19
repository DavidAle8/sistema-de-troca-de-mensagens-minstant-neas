package br.ufs.dcomp.ChatRabbitMQ;
import com.rabbitmq.client.*;


class ThreadConsumeMessage extends Thread {
    
    private Channel channel;
    private String user;
    
    public ThreadConsumeMessage(Channel channel, String user) {
        this.channel = channel;
        this.user = user;
    }
    
    public void run(){
        
        try{
            String msUser = "MS_" + user;
            
            Consumer consumer = ChatSerializer.consumeMessage(channel, msUser);
            channel.basicConsume(msUser, true, consumer);  
            
            synchronized(this){ 
                this.wait(); 
            }
            
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
