import java.util.*;

public class AttributesAndFD {
    private int sizeFD;
    private final Attributes attributes = new Attributes();
    private ArrayList<FunctionalDependency> functionalDependencies = new ArrayList<>();

    //Attributes:
    public void addAttributes(String input) {
        input = input.replaceAll(" ", "");
        char[] arr = input.toCharArray();

        for (char c : arr) {
            this.attributes.addAttributes(c);
        }
    }

    //Functional Dependencies
    public void initializeFD(int size) {
        this.sizeFD = size;
        this.functionalDependencies = new ArrayList<>(size);
    }

    public int getSizeOfFD() {
        return this.sizeFD;
    }

    public boolean addFD(String str) {
        String[] FDs = str.split("->");

        //left => FDs[0] and right => FDs[1]

        FunctionalDependency curr = new FunctionalDependency(FDs[0], FDs[1]);
        if(curr.correctFD(this.attributes.getAttributes())) {
            this.functionalDependencies.add(curr);
            return true;
        } else {
            return false;
        }
    }

    //3NF Synthesis with DP preserving
    public ArrayList<Attributes> bcnfSynthesis() {
        /**
         * Set D = {R}*/
        HashMap<String, ArrayList<FunctionalDependency>> decomposition = new HashMap<>();
        String attrString = StringFunctionHelper.convertSetToString(this.attributes.getAttributes());
        decomposition.put(attrString, this.functionalDependencies);

        /**
         * While there is a relation schema Q in D that is not in BCNF
         * do {
         ****** choose a relation schema Q in D that is not in BCNF;
         ****** find a functional dependency X->Y in Q that violated BCNF;
         ****** replace Q in D by two relation schemas (Q-Y) and (X u Y);
         *}*/

        boolean bcnf = false;
        while(!bcnf) {
            bcnf = true;

            for(Map.Entry<String, ArrayList<FunctionalDependency>> entry: decomposition.entrySet()) {
                //check if this entry is in bcnf
                String currAttr = entry.getKey();
                ArrayList<FunctionalDependency> fds = entry.getValue();

                // TODO: 19-03-2023 Complete bcnfChecker and get back here
                FunctionalDependency violatedFD = bcnfChecker(currAttr, fds);
                if(violatedFD != null) {
                    //we got a fd which violated bcnf
                    //replace the current entry with
                    // => currAttr - violatedFD.getRight()
                    // => violatedFD.getLeft() and violatedFD.getLeft()

                    String firstStringToAdd = stringSubtraction(currAttr, violatedFD.getRight());
                    String secondStringToAdd = stringUnion(violatedFD.getLeft(), violatedFD.getRight());

                    ArrayList<FunctionalDependency> firstFD = getFDwrtAttr(firstStringToAdd, this.functionalDependencies);
                    ArrayList<FunctionalDependency> secondFD = getFDwrtAttr(secondStringToAdd, this.functionalDependencies);

                    //Testing
//                    System.out.println("Violated fd = " + violatedFD);
//                    System.out.println("firstStringToAdd = " + firstStringToAdd);
//                    for(FunctionalDependency fd: firstFD) {
//                        System.out.println(fd);
//                    }
//
//                    System.out.println("secondStringToAdd = " + secondStringToAdd);
//
//                    for(FunctionalDependency fd: secondFD) {
//                        System.out.println(fd);
//                    }

                    decomposition.put(firstStringToAdd, firstFD);
                    decomposition.put(secondStringToAdd, secondFD);
                    decomposition.remove(entry.getKey());

                    bcnf = false;
                    break;
                }
            }
        }

        ArrayList<Attributes> result = new ArrayList<>();
        for(String str: decomposition.keySet()) {
            Attributes attr = new Attributes();
            for(int i=0; i<str.length(); i++) {
                attr.add(str.charAt(i));
            }
            result.add(attr);
        }

        String attr = StringFunctionHelper.convertSetToString(this.attributes.getAttributes());
        if(decompositionContainEverything(attr, result)) {
            ArrayList<Attributes> newResult = new ArrayList<>();
            newResult.add(this.attributes);
            return newResult;
        }

        return result;
    }

    private static boolean decompositionContainEverything(String attr, ArrayList<Attributes> decomposition) {
        for(Attributes dec: decomposition) {
            boolean value = true;
            for(int i=0; i<attr.length(); i++) {
                if (!dec.contains(attr.charAt(i))) {
                    value = false;
                    break;
                }
            }

            if(value) {
                return true;
            }
        }

        return false;
    }

    //will return list of functional dependency which are valid for the given set of attributes
    private ArrayList<FunctionalDependency> getFDwrtAttr(String attrString, ArrayList<FunctionalDependency> fds) {
        ArrayList<FunctionalDependency> result = new ArrayList<>();

        String sortedAttr = StringFunctionHelper.sortString(attrString);
        int length = sortedAttr.length();

        for(int i=0; i<(1<<length); i++) {
            StringBuilder stringBuilder = new StringBuilder();
            for(int j=0; j<length; j++) {
                if(((i>>j) % 2 == 1)) {
                    stringBuilder.append(sortedAttr.charAt(j));
                }
            }
            String currAttr = stringBuilder.toString();

            String closureString = closure(currAttr, fds);

            HashSet<Character> charThatShouldBeThere = new HashSet<>();
            for(int j=0; j<sortedAttr.length(); j++) {
                charThatShouldBeThere.add(sortedAttr.charAt(j));
            }

            HashSet<Character> charThatShouldNotBeThere = new HashSet<>();
            for(int j=0; j<currAttr.length(); j++) {
                charThatShouldNotBeThere.add(currAttr.charAt(j));
            }
//            System.out.println("charThatShouldBeThere = " + charThatShouldBeThere);
//            System.out.println("charThatShouldNotBeThere = " + charThatShouldNotBeThere);

            StringBuilder rightOfFD = new StringBuilder();
            for(int j=0; j<closureString.length(); j++) {
                char ch = closureString.charAt(j);
                if(charThatShouldBeThere.contains(ch)) {
                    if(!charThatShouldNotBeThere.contains(ch)) {
                        rightOfFD.append(ch);
                    }
                }
            }

            String rightString = rightOfFD.toString();
            if(rightString.length() != 0) {
                FunctionalDependency fdToAdd = new FunctionalDependency(currAttr,rightString);
                result.add(fdToAdd);
            }
        }

        return result;
    }

    //performs first - second
    private String stringSubtraction(String first, String second) {
        Attributes attr = StringFunctionHelper.convertStringToAttributes(second);
        HashSet<Character> set = attr.getAttributes();

        StringBuilder stringBuilder = new StringBuilder();
        for(int i=0; i<first.length(); i++) {
            if(!set.contains(first.charAt(i))) {
                stringBuilder.append(first.charAt(i));
            }
        }

        return stringBuilder.toString();
    }

    private String stringUnion (String first, String second) {
        HashSet<Character> set = new HashSet<>();
        char[] firstArray = first.toCharArray();
        char[] secondArray = second.toCharArray();

        for(char ch: firstArray) {
            set.add(ch);
        }

        for(char ch: secondArray) {
            set.add(ch);
        }
        return StringFunctionHelper.convertSetToString(set);
    }

    //returns null if given input is in bcnf,
    //otherwise returns the functional dependency which violates bcnf
    private FunctionalDependency bcnfChecker(String attr, ArrayList<FunctionalDependency> fds) {
        String attrSorted = StringFunctionHelper.sortString(attr);

        //for every functional dependency, X->Y X should be super key
        for(FunctionalDependency currFD: fds) {
            String left = currFD.getLeft();

            //find closure of this with respect to fds
            String closureString = closure(left, fds);
            String closureSorted = StringFunctionHelper.sortString(closureString);

            if(!attrSorted.equals(closureSorted)) {
                //is not a super key
                return currFD;
            }
        }
        return null;
    }

    private static String closure(String attr, ArrayList<FunctionalDependency> fd) {
        /**
         * X+ := X
         * repeat
         ****** oldX+ := X+
         ****** for each functional dependency Y->Z in F do
         ************* if X+ is subset of Y then X+:= X+ union Z
         * until (X+ = oldX+)*/


        HashSet<Character> result = new HashSet<>();

        for(int i=0; i<attr.length(); i++) {
            result.add(attr.charAt(i));
        }

        boolean modified = true;
        while(modified) {
            modified = false;

            for(FunctionalDependency currFD: fd) {
//                System.out.println("Checking - " + currFD);
                String left = currFD.getLeft();

                String resultString = StringFunctionHelper.convertSetToString(result);

                //check if result string contains left string
                if(StringFunctionHelper.stringContainsCharacters(resultString,left)) {
                    char[] rightArr = currFD.getRight().toCharArray();

                    for(char ch: rightArr) {
                        if (!result.contains(ch)) {
                            result.add(ch);
                            modified = true;
                        }
                    }
                }

//                System.out.println("Result after checking the above fd " + result);
            }
        }

        return StringFunctionHelper.convertSetToString(result);
    }

    @Override
    public String toString() {
        if(attributes.isEmpty()) {
            return "No attributes were added";
        } else if(functionalDependencies.isEmpty()) {
            return "Attributes are:\n" + attributes.getAttributes().toString();
        } else {
            StringBuilder stringBuilder = new StringBuilder("Attributes are:\n");
            stringBuilder.append(attributes.getAttributes().toString());

            stringBuilder.append("\n").append("Functional Dependencies are:\n");

            for(FunctionalDependency fd: functionalDependencies) {
                stringBuilder.append(fd.toString()).append("\n");
            }

            return stringBuilder.toString();
        }
    }


    //Testing
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        AttributesAndFD attributesFD = new AttributesAndFD();

        System.out.println(Colors.ANSI_RESET + "Enter all the attributes (space separated): ");
        String input = sc.nextLine();
        attributesFD.addAttributes(input);

        System.out.println(Colors.ANSI_PURPLE + attributesFD);

        System.out.println(Colors.ANSI_RESET + "Enter the number of functional dependency");
        int sizeOfFD = sc.nextInt();
        String backslash = sc.nextLine();
        attributesFD.initializeFD(sizeOfFD);


        System.out.println(Colors.ANSI_RESET + "Enter the functional dependency in following format");
        System.out.println("Format:- AB->D");

        for(int i=0; i<attributesFD.getSizeOfFD(); i++) {
            input = sc.nextLine();
            boolean addedCorrectly = attributesFD.addFD(input);
            if(!addedCorrectly) {
                System.out.println(Colors.ANSI_RED + "Entered functional dependency:- " + input + " is not correct, " +
                        "it is having attributes that were not declared.\n" +
                        "Please re-enter the functional dependency");
                i--;
            }
        }

//        System.out.println("Enter the string attribute:");
//        input = sc.nextLine();
//        System.out.println(closure(input, attributesFD.functionalDependencies));

        //testing the minimal cover
//        attributesFD.findMinimalCover();

//        HashSet<Attributes> superKey = attributesFD.findSuperKey();
//        for(Attributes att: superKey) {
//            System.out.println(att.getAttributes());
//        }

    }
}
