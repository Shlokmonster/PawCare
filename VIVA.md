# PAWCARE — Viva Notes

Everything you need to explain this project out loud, in the order you would explain it.

---

## Part 1 — The project in one minute

> "PAWCARE is a desktop application for a veterinary clinic, written in Java 17 with Swing.
> It keeps six kinds of record — patients, clients, veterinarians, appointments, treatments
> and vaccinations — and it links them together, so a patient's appointments, treatments
> and vaccinations all belong to that patient.
>
> The data is held in Java collections: an `ArrayList` for the patient list, a `HashMap`
> for instant lookup by id, a `TreeMap` for the appointment diary and a `LinkedList` per
> patient for the treatment history. Everything is saved to files using Java serialization,
> so the data is still there when the program is started again.
>
> The interesting part is the vaccination reminders. Every dose is classified as overdue,
> due soon or upcoming, and the dashboard shows what needs action today. All of those
> numbers are calculated from the live data — none of them are typed in.
>
> The interface is a Swing dashboard with nine screens, a navigation rail, a light and dark
> theme, tables with sortable columns, search, filters and confirmation dialogs."

---

## Part 2 — The two-minute walkthrough (the layered answer)

> "The project is in four layers.
>
> **The model layer** holds the data and the behaviour that belongs to it. `Animal` is an
> abstract class; `Pet` extends it; `Dog`, `Cat` and `Bird` extend `Pet`. That is
> inheritance and abstraction. `Animal` declares an abstract method `treatmentPlan()`, and
> each species implements it differently. When the interface shows a patient's clinical
> plan, it just calls `animal.treatmentPlan()` — it never asks which species it is holding.
> That is runtime polymorphism.
>
> **The service layer** owns the collections and every business rule. `PetService` keeps an
> `ArrayList<Pet>` for the list you see on screen and a `HashMap<String, Pet>` for lookup
> by id — that makes `findPetById` a constant-time hash probe instead of a scan through the
> list. `AppointmentService` keeps a `TreeMap<LocalDate, ArrayList<Appointment>>`, so the
> diary is sorted by date automatically. `TreatmentService` keeps one `LinkedList<Treatment>`
> per patient, because a medical history is appended to and read from start to finish.
>
> **The repository layer** is `DataStore`. It writes six files with
> `ObjectOutputStream`, and it writes to a temporary file first and then moves it into
> place, so a crash halfway through a save cannot destroy the previous good file.
>
> **The GUI layer** is Swing. `MainFrame` holds a `CardLayout` with the nine pages and a
> navigation rail. Every page asks `ClinicService` for data — the pages never touch a file
> and never re-implement a rule.
>
> `Theme` is the single place where the colours, fonts and spacing are defined, which is
> what makes the light/dark switch possible: changing the theme rebuilds every screen from
> the same definitions."

---

## Part 3 — Forty-five viva questions with short answers

### A. Object-oriented programming

**1. What is encapsulation, and where is it in your project?**
Encapsulation means the data of a class is private and can only be reached through its
methods. In every model class, for example `Animal`, all six fields are `private` and are
exposed through `getName()`, `setAge()` and so on. Because nothing outside can change a fee
or an id directly, the rules cannot be bypassed.

**2. What is abstraction? Give an example from your project.**
Abstraction means hiding the details and showing only what matters. `Animal` is an abstract
class — you cannot write `new Animal()`. It says every animal has a `treatmentPlan()`
without saying what that plan is; each species fills it in.

**3. What is inheritance, and how deep is your hierarchy?**
Inheritance lets a class reuse another class's fields and methods. Mine is three levels
deep: `Animal` → `Pet` → `Dog` / `Cat` / `Bird`. `Dog` inherits `name`, `age`, `weight`,
`gender` and `breed` from `Animal`, and `ownerId`, `registrationDate` and `medicalInfo`
from `Pet`.

**4. What is polymorphism?**
Polymorphism means one call can behave differently depending on the actual object. In my
project `Animal.treatmentPlan()` is abstract and each subclass overrides it. A variable of
type `Animal` holding a `Cat` runs the feline plan; holding a `Bird` it runs the avian plan.
The decision is made at runtime by the JVM.

**5. What is the difference between method overloading and method overriding?**
Overloading is several methods with the same name but different parameter lists in the same
class — for example `ValidationUtil.isBlank(String)` is one method while the different
`Dialogs.error(…)` signatures coexist in one class. Overriding is a subclass replacing a
method it inherited — `Pet.displayInfo()` overrides `Animal.displayInfo()`. Overloading is
decided at compile time, overriding at runtime.

**6. What is the difference between an abstract class and an interface?**
An abstract class can hold fields, constructors and implemented methods, and a class can
extend only one of them. An interface declares behaviour that any number of classes can
implement. I use an abstract class (`Animal`) because the species share real state, and an
interface (`Serializable`) because "can be written to a file" is a capability, not a family
tree.

**7. Why can't you create an object of an abstract class?**
Because it is deliberately incomplete — an abstract class may have abstract methods with no
body, so an object of it could be asked to do something that has no implementation.

**8. What does a constructor do, and what is constructor chaining?**
A constructor initialises a new object. Constructor chaining is one constructor calling
another with `this(...)` or calling the parent with `super(...)`. `Dog`'s constructor calls
`super(...)`, which calls `Pet`'s constructor, which calls `Animal`'s — so the fields are
filled in from the top of the hierarchy downwards.

**9. Why did you add a no-argument constructor to every model class?**
Because Java serialization needs one to rebuild an object while reading a file. If it is
missing, deserialization fails.

**10. What is `this` used for?**
To refer to the current object. In my constructors it distinguishes the parameter from the
field: `this.ownerId = ownerId`.

**11. What is `super` used for?**
To refer to the parent class. `Pet.displayInfo()` calls `super.displayInfo()` and appends
the owner, so the parent's formatting is reused instead of copied.

**12. What is the difference between `==` and `equals()`?**
`==` compares references — whether two variables point at the same object. `equals()`
compares content and can be overridden. My model classes override `equals()` so two
`Treatment` objects with the same treatment id are equal even though they are different
objects in memory.

**13. Why must you override `hashCode()` when you override `equals()`?**
Because hash-based collections use the hash code to decide which bucket an object goes in.
If two equal objects had different hash codes, a `HashMap` would fail to find one that was
stored. My classes derive the hash code from the same id used by `equals()`.

**14. What is a `static` member?**
It belongs to the class, not to an object. `ValidationUtil.validatePet(...)` and
`IDGenerator.nextPetId()` are static because they are utilities that hold no per-object
state.

**15. Why do your utility classes have a private constructor?**
To stop anyone creating an object of a class that is only a container of static methods.
`ValidationUtil`, `DateUtil`, `IDGenerator` and `SampleData` all do this, and the classes
are `final` so they cannot be extended either.

**16. What is an inner class, and where do you use one?**
A class declared inside another class. `SettingsPage.Dot` is a small private class that
paints an 8-pixel coloured circle, and `Page.ScrollableBody` implements `Scrollable` so the
page scrolls vertically but not horizontally.

---

### B. Collections

**17. Which collections does your project use, and why each one?**
`ArrayList` for the pet and owner registers — I need indexed access, iteration and sorting.
`HashMap` for lookup by id, giving O(1) instead of a linear scan. `TreeMap` for the
appointment diary, so dates stay sorted automatically. `LinkedList` for each patient's
treatment history, because it is appended to and walked end to end. `EnumMap` for the
species and status counts, since the keys are enum constants. `TreeSet` on the Settings
page so the reminder choices come out in order with no duplicates.

**18. Why do you keep both an `ArrayList` and a `HashMap` for pets?**
The list preserves the order records were added and supports sorting and iteration for the
table; the map makes `findPetById` a constant-time lookup. They are updated together in
`addPet`, `updatePet`, `deletePet` and `loadAll`, so they can never drift apart.

**19. What is the difference between `ArrayList` and `LinkedList`?**
`ArrayList` is backed by an array: fast random access by index, but inserting or removing in
the middle shifts elements. `LinkedList` is a chain of nodes: appending and removing is
cheap, but reaching the tenth element means walking nine links.

**20. Why did you choose `LinkedList` for treatments?**
Because that is exactly how a medical history is used: a new entry is appended at the end,
and the history is read from the oldest entry to the newest. Random access by index is never
needed, so the `ArrayList` advantage does not apply, and the O(1) append of a linked list
does.

**21. What is a `TreeMap`, and why is it right for the appointment diary?**
A `TreeMap` keeps its keys in sorted order at all times. Because the keys are `LocalDate`
and `LocalDate` implements `Comparable`, the diary is always in date order without a single
explicit sort call, and `scheduledDates()` is just the key set.

**22. What is an `EnumMap`, and why use it instead of a `HashMap`?**
A map specialised for enum keys. It is backed by an array, so it is faster and smaller, and
it keeps the keys in the order the enum constants were declared — which is exactly the
order the charts want.

**23. What is the difference between `HashMap` and `Hashtable`?**
`HashMap` allows one null key and is not synchronised. `Hashtable` allows no null keys and
is synchronised, which makes it slower. The modern replacement for a thread-safe map is
`ConcurrentHashMap`. I use `HashMap`.

**24. What is the difference between `Comparable` and `Comparator`?**
`Comparable` is implemented by the class itself and gives it one natural order —
`Treatment implements Comparable<Treatment>` with `compareTo`. A `Comparator` is a separate
object holding an alternative order, so a class can be sorted in many ways without changing.
`Treatment.BY_DATE` is a comparator used alongside the natural order.

**25. How do you sort in your project?**
Three ways, deliberately. `PetService.sortedByName()` uses the classic
`Collections.sort(list, comparator)`. `PetService.sortedBy(comparator)` uses the modern
`List.sort(...)`. And `AppointmentService` does not sort at all — the `TreeMap` is already
ordered.

**26. How does `HashMap` find a key so quickly?**
It computes `hashCode()` of the key, converts it to a bucket index, and looks only inside
that bucket, comparing with `equals()`. That is why `findPetById("P042")` does not walk the
list.

**27. Why does `Map.merge` appear in your counting code?**
Because counting per key is exactly what it is for:
`counts.merge(pet.getSpecies(), 1, Integer::sum)` adds one, or starts at one if the key is
new. It replaces a loop with a null check.

**28. What is `computeIfAbsent` used for in your code?**
To create the list or map for a key the first time it is needed:
`historyByPet.computeIfAbsent(petId, key -> new LinkedList<>()).addLast(treatment)`. Without
it, every such line would need an `if (list == null)` check.

**29. What is the difference between `List` and `Set`?**
A `List` is ordered and allows duplicates; a `Set` does not allow duplicates. I use a `Set`
(`TreeSet`) for the reminder-day choices so the same value is never offered twice.

**30. What is the difference between `Collection` and `Collections`?**
`Collection` is the interface that `List`, `Set` and `Queue` extend. `Collections` is a
utility class full of static helpers, such as `Collections.sort(...)`.

**31. How do you stop a caller from modifying your internal list?**
`getAllPets()` returns `new ArrayList<>(pets)` — a copy. The caller can do whatever they
like to the copy and the store is unaffected. There is a test for exactly this.

**32. What is an `Iterator`, and where does it matter in your code?**
An object that walks a collection. It matters because you must not modify a collection while
iterating it directly — that throws `ConcurrentModificationException`. Where I remove
entries I collect the ones to remove first and then remove them, which is why
`deleteByPet` builds a list before deleting.

---

### C. Exceptions

**33. What is the difference between a checked and an unchecked exception?**
A checked exception extends `Exception` and the compiler forces you to handle or declare it.
An unchecked one extends `RuntimeException` and is not enforced. My whole
`PawCareException` family is checked — that is deliberate, because a validation failure must
not be ignored.

**34. Why are your custom exceptions checked rather than unchecked?**
Because a caller cannot accidentally forget to handle them. If `addPet` can fail validation,
the compiler makes whoever calls it deal with that fact.

**35. Show your exception hierarchy.**
`PawCareException extends Exception`, and six subclasses extend it:
`InvalidPetException`, `InvalidOwnerException`, `InvalidAppointmentException`,
`InvalidTreatmentException`, `InvalidVaccinationException`, `EntityNotFoundException` and
`DataAccessException`. Unchecked `IllegalStateException` is used only for a bug in the
program itself — invalid bundled sample data.

**36. How can one exception carry several messages?**
`PawCareException` has a `List<String> details`, and `getDetailedMessage()` formats the
message followed by one bullet per problem. `ValidationUtil` collects every problem before
throwing, so a user sees all six mistakes in one dialog instead of fixing them one at a
time.

**37. What is exception chaining, and why do you use it?**
Passing the original exception as the cause: `new DataAccessException("Could not save …", e)`.
The user sees a clear sentence while the original `IOException` is still available in the
stack trace for debugging.

**38. Why does your code never contain `catch (Exception e) {}`?**
Because it would hide every problem, including bugs. Every `catch` in the project names the
specific exceptions it expects — for example
`catch (IOException | ClassNotFoundException e)` in `DataStore.load`.

**39. What is `try`-with-resources, and where do you use it?**
It closes anything that implements `AutoCloseable` automatically, even if the block throws.
`DataStore.save` and `load` use it for the file streams, and `AppSettings.load` for the
input stream.

**40. What happens if a `data/*.dat` file is corrupt?**
`DataStore.load` throws `DataAccessException`. `ClinicService` catches it, moves the damaged
file aside as `<name>.corrupt-<timestamp>` so it can be inspected, records a warning in
`loadWarnings`, and continues with an empty list. The window opens and shows the warning in
a dialog — the program is never allowed to fail to start.

---

### D. Strings, dates and files

**41. Why do you use `LocalDate` instead of `java.util.Date`?**
`LocalDate` is immutable, thread-safe and unambiguous — it holds only a date with no time
and no time zone. `Date` is mutable and mixes in a time and a default time zone, which
causes bugs.

**42. Why is `String` immutable, and why does it matter here?**
A `String` cannot be changed after it is created; every operation returns a new one. That
makes strings safe to share and to use as `HashMap` keys — which is exactly how pet ids and
owner ids are used as keys.

**43. What is the difference between `String`, `StringBuilder` and `StringBuffer`?**
`String` is immutable. `StringBuilder` is mutable and fast for building text in a loop.
`StringBuffer` is the same but synchronised, so slower. I use `String.format` and
`StringBuilder` where text is assembled.

**44. How do you write and read your data files?**
With Java serialization: `ObjectOutputStream.writeObject(list)` and
`ObjectInputStream.readObject()`. Every model class implements `Serializable` and declares a
`serialVersionUID`. The `DataStore` writes each entity type into its own file inside
`data/`.

**45. Why do you write to a temporary file and then move it?**
So a crash in the middle of a save cannot destroy the file that is already there. The write
goes to `pets.dat.tmp`; only when it is complete is it moved over `pets.dat` with
`Files.move(..., ATOMIC_MOVE)`. At every moment, `pets.dat` is either the old complete file
or the new complete file.

---

### E. Swing and the interface

**46. What is the event dispatch thread, and why does it matter?**
Swing is single-threaded: every component must be created and changed on the event dispatch
thread. `Main.main` therefore calls `SwingUtilities.invokeLater(Main::launch)`, so the whole
interface is built on that thread. Touching Swing from another thread causes unpredictable
failures.

**47. Why did you choose the cross-platform look and feel?**
Because it is drawn entirely in Java, so it looks the same on Windows, macOS and Linux, and
its colours can be replaced through `UIManager` — which is what makes the light/dark theme
possible.

**48. What is a layout manager, and which ones do you use?**
A layout manager decides where components go and how they resize. I use `BorderLayout` for
the frame, `BoxLayout` for the vertical stack of cards on each page, `CardLayout` to swap
pages, `GridLayout` for rows of equal cards, `GridBagLayout` inside form dialogs and
`FlowLayout` for button rows.

**49. How does the theme switching work?**
`Theme` holds the current palette, fonts and spacing in one place, and `ThemeManager`
changes it and notifies its listeners, persisting the choice in `settings.properties`.
`MainFrame` is a listener: it rebuilds its content pane, which recreates every page with the
new colours. Swing components keep the colours they were constructed with, so rebuilding is
the only reliable way to repaint.

**50. How do you make the tables sortable?**
`ModernTable` wraps its model in a `TableRowSorter` and supplies a comparator per column, so
clicking a header sorts by the real value — a date column sorts chronologically, not
alphabetically.

**51. What is the difference between `JFrame` and `JDialog`, and where do you use each?**
A `JFrame` is a top-level window with minimise and maximise buttons; a `JDialog` is
attached to a parent window and can be modal. `MainFrame` is the only `JFrame`; every form —
adding a pet, an owner, an appointment, a treatment or a vaccination — is a modal
`JDialog`, so the user cannot edit the table behind it.

**52. What is `CardLayout` used for in your project?**
For the nine screens. All nine are created once and added to one container; navigating
simply tells the card layout which one to show. Switching is instant and no data is copied.

**53. How do you paint a rounded card or a custom button?**
By overriding `paintComponent(Graphics)` and drawing with `Graphics2D` — filling a
`RoundRectangle2D` for the background, then drawing the text and the icon. Every one of
those methods turns on antialiasing so the edges are smooth.

**54. What is `contentAreaFilled(false)` for?**
It tells a `JButton` not to paint its own default background, so my custom-painted background
is the only thing visible. Without it the default look-and-feel rectangle is painted over my
drawing.

**55. How does the vaccination reminder window reach every screen?**
`VaccinationService.getReminderDays()` reads the value live from `AppSettings` rather than
holding a copy. So changing it on the Settings page immediately changes the classification
on the dashboard, the patient table and the vaccination page, with nothing to reload.

---

### F. Design and general questions

**56. Why is there a `ClinicService` instead of the pages using the services directly?**
So that a page depends on one object instead of eight, and so that "validate, then save to
disk at once" happens in exactly one place. It is a façade.

**57. What is the difference between composition and inheritance?**
Inheritance is "is a" — a `Dog` is a `Pet`. Composition is "has a" — `SearchService` *has* a
`PetService` and an `OwnerService` and uses them. I use inheritance for the species and
composition everywhere else, because inheritance is a strong coupling.

**58. Why do your records store an owner id instead of an `Owner` object?**
Because that keeps the stored data normalised. If the owner's phone number changes, one
owner record is updated instead of every pet that pointed at the owner. It also stops
serialization from writing the same owner object many times.

**59. How do you avoid orphan records when a patient is deleted?**
`ClinicService.deletePet` deletes the pet's appointments, treatments and vaccinations first
and then the pet itself, and writes all four files. A test checks that the cascade really
happens and that it survives a restart.

**60. How is the "recent treatments" list on the dashboard computed?**
`TreatmentService.allNewestFirst()` sorts every treatment by date descending and
`recent(limit)` takes the first few. Nothing is cached or hard-coded.

**61. What is the time complexity of your main operations?**
Lookup by id is O(1) through the `HashMap`. Adding a pet is O(1) amortised. Listing all pets
is O(n). Searching by name is O(n) because it must examine every pet. Getting a patient's
treatment history is O(k log k) where k is that patient's entries, because the copy is
sorted. The appointment diary never sorts — the `TreeMap` maintains order, so insertion is
O(log d) in the number of distinct days.

**62. How would you extend this project to a real clinic with several reception desks?**
I would move the service layer behind a server process and have the Swing client talk to it,
and replace the serialized files with a real database so that concurrent writes are safe.
The model, validation and report layers would not have to change at all — which is the
point of keeping them separate.

**63. What is the most interesting bug you had to fix?**
The page layout. In a `BoxLayout`, a component's `setMaximumSize` decides how much it may
grow — not its preferred size. When the total preferred height of the cards exceeded the
visible window, `BoxLayout` shrank every card towards its minimum height and the tables
collapsed. The fix was to pin each card to a fixed height inside a fixed-height row, so the
layout cannot squeeze them. This is why `Page` has both `stack(...)` (fixed) and
`stackGrowing(...)`.

**64. What would you do differently if you started again?**
I would put the validation rules on the model classes themselves rather than in a
`ValidationUtil`, and I would add a test for the interface as well as for the services. I
would also introduce the theme colours later — doing it from the first screen would have
saved repainting work.

**65. Which part of the project are you most confident about, and why?**
The service layer, because it is the part with the clearest rules, and every rule has a unit
test. The tests use `@TempDir`, so they never touch the real `data/` folder.

---

## Part 4 — Five-minute demonstration script

Run the program with `java -jar target/pawcare.jar`.

| Time | What to do | What to say |
|------|-----------|-------------|
| 0:00 | The Dashboard opens | "This is the dashboard. Every number here is computed from the live collections — none of it is typed in." |
| 0:20 | Point at **Today's appointments** | "These come from a `TreeMap<LocalDate, …>`, so they were already in date order." |
| 0:45 | Point at **Vaccination alerts** | "Overdue means the next dose was due before today; due soon means it falls inside the reminder window of thirty days." |
| 1:00 | Go to **Pets** | "Eight patients across three species. The badge in the Status column is derived from that patient's vaccinations, not stored." |
| 1:20 | Open a patient, show the **clinical plan** | "This text comes from `animal.treatmentPlan()`. The interface does not know which species it is looking at — each subclass supplies its own plan. That is runtime polymorphism." |
| 1:50 | Click **Add patient**, enter a bad phone number and a one-letter name in the owner dialog | "Both problems are reported in one dialog, because `ValidationUtil` collects every failure before throwing." |
| 2:20 | Add a **patient**, then go to **Appointments** and add two visits for the same vet at the same time | "The second one is refused. That is the double-booking rule; a cancelled slot is free again." |
| 2:50 | Go to **Vaccinations** | "An overdue dose, one due this week and one months away — the three states, computed by one enum method." |
| 3:10 | Go to **Search**, search `bru` with all fields, then switch the scope to **Pet ID** | "Same query, different scope. Each scope only looks at its own field." |
| 3:30 | Go to **Reports** | "Totals, breakdowns and the top clients — all derived from the same services." |
| 3:50 | Go to **Settings**, change the reminder window from 30 to 90 days | "Watch the vaccination page." |
| 4:05 | Back to **Vaccinations** | "Records that were 'upcoming' are now 'due soon'. The service reads the window live from the preferences, so nothing had to be reloaded." |
| 4:20 | Switch to **Dark** | "The whole interface is rebuilt from one `Theme` class, so one flag changes every colour." |
| 4:40 | Close the window and start the program again | "Everything is still here — the data is written to `data/*.dat` with Java serialization as each change happens." |
| 5:00 | In a terminal, `mvn test` | "The suite covers validation, the collections, the reminder rule, persistence and the cascading deletes." |

---

## Part 5 — Things to have ready

* **Numbers to know:** 5 clients, 4 veterinarians, 8 patients, 8 appointments, 10 treatments,
  10 vaccinations. The reminder window defaults to **30 days** and can be set from 7 to 90.
* **File to point at for validation:** `src/main/java/com/pawcare/util/ValidationUtil.java`.
* **File to point at for polymorphism:** `model/Animal.java` (the abstract method) and
  `model/Dog.java` (the override).
* **File to point at for the collections:** `service/PetService.java` (ArrayList + HashMap),
  `service/AppointmentService.java` (TreeMap), `service/TreatmentService.java` (LinkedList).
* **File to point at for persistence:** `repository/DataStore.java`.
* **File to point at for the theme:** `theme/Theme.java`.
* **If you are asked to show a test:** `service/VaccinationServiceTest.java` for the reminder
  rules, or `repository/DataStoreTest.java` for the corrupt-file recovery.

**The five sentences worth memorising:**

1. "`Animal` is abstract, `Dog`, `Cat` and `Bird` extend `Pet`, and the interface calls
   `treatmentPlan()` without knowing the species — that is runtime polymorphism."
2. "The pet register is an `ArrayList` for order and a `HashMap` for O(1) lookup by id, and
   both are updated together so they can never disagree."
3. "The appointment diary is a `TreeMap<LocalDate, ArrayList<Appointment>>`, so it is always
   sorted by date without sorting anything."
4. "Validation collects every problem and throws one checked exception carrying the whole
   list, so the user sees all the mistakes at once."
5. "Every change is written straight to a `data/*.dat` file through a temporary file and an
   atomic move, so a crash can never destroy the previous good data."
