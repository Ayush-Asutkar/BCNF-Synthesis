import java.util.ArrayList;
import java.util.HashSet;
import java.util.Scanner;

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


        return null;
    }

    private String minLengthAttributeFinder(HashSet<String> attSet) {
        String minSizeKey = null;
        int minLength = Integer.MAX_VALUE;

        for(String att: attSet) {
            int attLength = att.length();

            if(minLength > attLength) {
                minSizeKey = att;
                minLength = attLength;
            }
        }

        return minSizeKey;
    }

    private HashSet<String> findSuperKey() {
        HashSet<String> result = new HashSet<>();

        String attrString = StringFunctionHelper.convertSetToString(attributes.getAttributes());
        String sortedAttrString = StringFunctionHelper.sortString(attrString);
        int length = sortedAttrString.length();

        for(int i=0; i<(1<<length); i++) {
            StringBuilder stringBuilder = new StringBuilder();
            for(int j=0; j<length; j++) {
                if(((i >> j) % 2 == 1)) {
                    stringBuilder.append(sortedAttrString.charAt(j));
                }
            }
            String currAttr = stringBuilder.toString();
//            System.out.println("CurrAttr = " + currAttr);

            String closureString = closure(currAttr, functionalDependencies);
            String closureStringSorted = StringFunctionHelper.sortString(closureString);

            if(sortedAttrString.equals(closureStringSorted)) {
//                System.out.println("Super key = " + currAttr);
                Attributes toBeAddedToSuperKey = new Attributes();
                for(int k=0; k<currAttr.length(); k++) {
                    toBeAddedToSuperKey.add(currAttr.charAt(k));
                }
                String resToAdd = StringFunctionHelper.convertSetToString(toBeAddedToSuperKey.getAttributes());
                String sortedRes = StringFunctionHelper.sortString(resToAdd);
                result.add(sortedRes);
            }
        }
        return result;
    }

    private ArrayList<FunctionalDependency> findMinimalCover() {
        /**
         * Set minimal = functional dependencies*/
        ArrayList<FunctionalDependency> minimal = new ArrayList<>();

        /**
         * Replace each functional dependency X->{A1,A2,A3,...An} in F by the n
         ******** functional dependencies X->A1, X->A2, X->A3, ..., X->An*/
        for (FunctionalDependency functionalDependency : functionalDependencies) {
            String left = functionalDependency.getLeft();
            String right = functionalDependency.getRight();

            if (right.length() == 1) {
                minimal.add(new FunctionalDependency(left, right));
            } else {
                char[] rightArr = right.toCharArray();
                for (char ch : rightArr) {
                    minimal.add(new FunctionalDependency(left, String.valueOf(ch)));
                }
            }
        }

//        System.out.println("After decomposing right side => " + minimal);
        //working correctly till here


        /**
         * For each functional dependency X->A in F
         ******* For each attribute B that is an element of X
         **************** if {{F - {X -> A}} U {(X - {B}) -> A}} is equivalent to F
         **************** then replace x->A with (X - {B}) -> A in F*/
        for (int i=0; i<minimal.size(); i++) {
            String left = minimal.get(i).getLeft();
            String right = minimal.get(i).getRight();

            if(left.length() == 1) {
                continue;
            }

            for(int j=0; j<left.length(); j++) {
                ArrayList<FunctionalDependency> mightBeMinimal = new ArrayList<>(minimal);

                mightBeMinimal.remove(new FunctionalDependency(left, right));

                StringBuilder newLeft = new StringBuilder();
                for(int k=0; k<left.length(); k++) {
                    if (k != j) {
                        newLeft.append(left.charAt(k));
                    }
                }

                mightBeMinimal.add(new FunctionalDependency(newLeft.toString(), right));

                if(equivalenceCheckerFD(minimal, mightBeMinimal)) {
                    //both are equivalent
                    //remove the functional dependency and add the smaller one
                    minimal.remove(new FunctionalDependency(left, right));
                    minimal.add(new FunctionalDependency(newLeft.toString(), right));
//                    System.out.println("i = " + i);
                    i--;
                    break;
                }

                mightBeMinimal.clear();
            }
        }

//        System.out.println("After remove extraneous attribute from left side=> " + minimal);

        for(int i=0; i<minimal.size(); i++) {
            String left = minimal.get(i).getLeft();
            String right = minimal.get(i).getRight();

            ArrayList<FunctionalDependency> mightBeMinimal = new ArrayList<>(minimal);

            mightBeMinimal.remove(new FunctionalDependency(left, right));

            if(equivalenceCheckerFD(minimal, mightBeMinimal)) {
                //remove the current functional dependency
                minimal.remove(i);
                i--;
            }
        }

//        System.out.println("After removing redundant dependency => " + minimal);
        return minimal;
    }

    private boolean equivalenceCheckerFD(ArrayList<FunctionalDependency> first, ArrayList<FunctionalDependency> second) {
        /** Test first covers second*/
        boolean firstCoversSecond = coverChecker(first, second);

        /** Test second covers first*/
        boolean secondCoversFirst = coverChecker(second, first);

        return firstCoversSecond && secondCoversFirst;
    }

    private boolean coverChecker(ArrayList<FunctionalDependency> first, ArrayList<FunctionalDependency> second) {
        /**
         * Step - 1:- For each functional dependency, x->y in second calculate x+ with respect to second
         * Step - 2:- For each functional dependency, x->y in second calculate x+ with respect to first
         * If all attributes determined by step 1 is subset of all attributes determined by step 2
         * => first covers second*/

        for(FunctionalDependency fd : second) {
            String left = fd.getLeft();
            String right = fd.getRight();

            String closureWRTSecond = closure(left, second);
            String closureWRTFirst = closure(left, first);

            if(!StringFunctionHelper.stringContainsCharacters(closureWRTFirst, closureWRTSecond)) {
                return false;
            }
        }

        return true;
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
