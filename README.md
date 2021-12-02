# AtmSimulator
AtmSimulator using sqlite as the storage mechanism

#requirements
1) JVM 11+
2) gradle 7

# to run tests
`./gradlew clean test `

#to create an account   
`./gradlew -PmainClass=sandbox.Application run --args="action=Createaccount username=<username> pin=<pin>" `

#to login a user
`./gradlew -PmainClass=sandbox.Application run --args="action=login username=<username> pin=<pin>" `

# to view a balance
`./gradlew -PmainClass=sandbox.Application run --args="action=viewbalance token=<token>`

# to make a deposit, in cents 
`./gradlew -PmainClass=sandbox.Application run --args="action=deposit amount=<amount> token=<token>`

# to make a withdrawal, in cents    
`./gradlew -PmainClass=sandbox.Application run --args="action=withdraw amount=<amount> token=<token>`
