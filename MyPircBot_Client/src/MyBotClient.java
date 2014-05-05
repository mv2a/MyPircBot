import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

import java.io.File;


public class MyBotClient {


    private static String botsNames[];

    private static int nicksCount;


    public static void main(String[] args) {

        if (args.length < 1) {
            System.out.println("Usage: MyBotClient <config>");
            System.exit(1);
        }

        File configFile = new File(args[0]);

        PropertiesConfiguration c = new PropertiesConfiguration();

        try {
            c.load(configFile);
        } catch (ConfigurationException e) {
            e.printStackTrace();
            System.exit(1);
        }

        if (c.containsKey("BotNicks")){
            botsNames = c.getStringArray("BotNicks");
        }

        nicksCount = botsNames.length - 1;


        while(true){
            if (nicksCount < 0)
                break;

            c.setProperty("Nick",botsNames[nicksCount]);

            try {
                Thread.sleep(c.getInt("ConDelay"));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            createNewInstance(c);

            nicksCount--;
        }

    }

    private static void createNewInstance(PropertiesConfiguration c){

        MyBot myBot = new MyBot(c.containsKey("Masters")?c.getStringArray("Masters"):null,
                c.containsKey("CommandPrefix")?c.getString("CommandPrefix"):null);

        try {
            myBot.initBot(c);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
    }

}
