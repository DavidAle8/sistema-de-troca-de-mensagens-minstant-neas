package br.ufs.dcomp.ChatRabbitMQ;
import com.rabbitmq.client.*;

class ThreadConsumeUpload extends Thread {
    
    private String user;
    private Channel channel;
    
    public ThreadConsumeUpload(Channel channel, String user){
        this.user = user;
        this.channel = channel;
    }
    
    public void run(){
        
        try{
            String upUser = "UP_" + user;
            
            Consumer consumer = ChatSerializer.consumeFile(channel, upUser);
            channel.basicConsume(upUser, true, consumer);
            
            synchronized(this){ 
                this.wait(); 
            }
            
        }catch(Exception e){
            e.printStackTrace();
        }
    }
}
