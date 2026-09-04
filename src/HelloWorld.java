public class HelloWorld {
    public static void main(String[] args) {
        if (args.length == 0)
            System.out.println("Hello world");
        else {
            System.out.print("Hello ");
            for (String aWord : args) {System.out.println(aWord+" ");}
            System.out.println("!");
        }
    }
}
