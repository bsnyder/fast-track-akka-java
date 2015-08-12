ask-pattern

# Exercise 19 > Ask Pattern

This exercise will demonstrate how to interact with an actor from the outside by way of the `ask` pattern.

- Change `CoffeeHouse` as follows:
    - Add the `GetStatus.Instance` message.
    - Add the `Status` message class that has a `guestCount` parameter of type `Int`.
    - On receiving `GetStatus` respond with `Status` initialized with the current number of `Guest` actors.
- Change `CoffeeHouseApp` as follows:
    - Handle a `StatusCommand` by asking `CoffeeHouse` to get the status.
    - Register callbacks logging the number of `Guest` actors at `info` and any failure at `error`.
    - For the `ask` timeout, use a configuration value with key `coffee-house.status-timeout`.
- Use the `run` command to boot the `CoffeeHouseApp` and verify everything works as expected.
- Use the `test` command to verify the solution works as expected.
- Use the `koan next` command to move to the next exercise.
