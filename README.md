Я вкратце расскажу стартовые экраны для приложения. Общее для жильца и админа.
Если он не авторизован, он переходит на экран подтверждения TermsAndConditionScreen.
После подтверждения на экран входа SignInScreen.
Если у него нет аккаунта он переходит на экран SignUpScreen.
Если аккаунт есть, то с помощью ApartmentNavGraph он переходит
для добавления квартиры или ввода секретного кода для админа на экран AddApartmentScreen.
Если у жильца есть квартира, а админ зарегистрирован, то жилец попадает на экран InfoApartmentScreen,
а админ на экран списка выбора чатов квартир UserListScreen. Пока реализуем это

This is a Kotlin Multiplatform project targeting Android, iOS, Web, Desktop (JVM).
Clean Architecture + MVI/MVVM в KMP
1.Repository: Чистая «труба» в интернет. Только Ktor, никаких обращений к SQLDelight.
2.QLDelight:Лежит в commonMain, генерирует методы, но вызывается только через лямбды.
3.UseCase: Единственное место, где принимается решение: «Взять из сети или отдать кэш».
4.Koin: «Клей», который подставляет в UseCase конкретные вызовы к базе.

После изьунения в SQLDelight
в терминале
./gradlew clean
./gradlew generateSqlDelightInterface
