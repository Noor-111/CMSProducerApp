package clients;


import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import clients.avro.Product;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.serializers.KafkaAvroSerializerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

public class Producer {

    static final String PRODUCTS_FILE_PREFIX = "./products/";
    static final String KAFKA_TOPIC = "products";

    private static long recordCount = 0;
    private static final long TIME_INTERVAL = 100;

    public static void main(String[] args) throws IOException, InterruptedException {
        System.out.println("Starting Java producer.");

        String clientId  = "products";//System.getenv("")

        // Configure properties
        final Properties settings = new Properties();
        settings.put(ProducerConfig.CLIENT_ID_CONFIG, clientId);
        // TODO: configure the location of the bootstrap server
        settings.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        settings.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        settings.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        settings.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, "http://localhost:8081");

        final KafkaProducer<String, Product> producer = new KafkaProducer<>(settings);

        // Adding a shutdown hook to clean up when the application exits
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Closing producer.");
            producer.close();
        }));

        final String[] rows = Files.readAllLines(Paths.get(PRODUCTS_FILE_PREFIX + "products.csv"),
                Charset.forName("UTF-8")).toArray(new String[0]);


        // Monitor thread for publishing rate
        Thread monitorThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(TIME_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                double rate = (double) recordCount / (double) TIME_INTERVAL;
                System.out.printf("recordCount = %d\n",recordCount);
                System.out.printf("[RATE] Publishing rate of events per 1ms : %.2f%n", rate);
                recordCount = 0;
            }
        });
        monitorThread.setDaemon(true);
        monitorThread.start();

        // Loop over the products CSV file
        try{
            int testCount=200000;
            long startTime = System.currentTimeMillis();
            for(int i=1;i<testCount;i++){
                final String key = rows[i].split(",")[0];// make PogId as key
                Product product = rowProductMapper(rows[i]);
                final ProducerRecord<String, Product> record = new ProducerRecord<>(KAFKA_TOPIC, key, product);
                producer.send(record, (md, e) -> {
                    if(e!=null){
                        e.printStackTrace();
                    } else {
                        //System.out.printf("Produced Key:%s Value:%s\n", record.key(), record.value());
                        //System.out.printf(" Record MetaData topic:%s partition:%s offset:%s\n",md.topic(), md.partition(),md.offset());
                        recordCount++;
                    }
                });
                //Thread.sleep(100);
            }
            long endTime = System.currentTimeMillis();

            Thread.sleep(10000);
            System.out.println("[TIME] Time taken = "+(endTime-startTime)+"ms");
            Thread.sleep(10000);
        } finally {
            producer.flush();
            producer.close();
        }



    }

    private static Product rowProductMapper(String row) {
        Product product = new Product();
        String[] fields = row.split(",");

        product.setPogId(Long.parseLong(fields[0].trim()));
        product.setSupc(fields[1].trim());
        product.setBrand(fields[2].trim());
        product.setDescription(fields[3].trim());
        product.setSize(fields[4].trim());
        product.setCategory(fields[5].trim());
        product.setSubCategory(fields[6].trim());
        product.setPrice(Float.parseFloat(fields[7].trim()));
        product.setQuantity(Integer.parseInt(fields[8].trim()));
        product.setCountry(fields[9].trim());
        product.setSellerCode(fields[10].trim());
        product.setCreationTime(Long.parseLong(fields[11].trim()));
        product.setStock(fields[12].trim());

        return product;
    }
}