# Contributing to HalalCraft

Thank you for considering contributing to HalalCraft! This document provides guidelines for contributing to the project.

## 📋 Code of Conduct

- Be respectful and considerate
- Follow Islamic values and principles
- Ensure all contributions promote halal gameplay
- Test thoroughly before submitting

## 🐛 Reporting Bugs

When reporting bugs, please include:

1. **Plugin Version**: Check with `/version HalalCraft`
2. **Minecraft Version**: Server version
3. **Server Type**: Spigot or Paper
4. **Description**: Clear description of the issue
5. **Steps to Reproduce**: Detailed steps
6. **Expected Behavior**: What should happen
7. **Actual Behavior**: What actually happens
8. **Error Logs**: Any console errors
9. **Screenshots**: If applicable

## 💡 Suggesting Features

Feature suggestions are welcome! Please provide:

1. **Clear Description**: What the feature does
2. **Islamic Basis**: How it relates to Islamic practices (if applicable)
3. **Use Case**: Why it would be useful
4. **Implementation Ideas**: Any technical thoughts

## 🔧 Development Setup

### Prerequisites

- Java JDK 17 or higher
- Maven 3.6+
- IDE (IntelliJ IDEA, Eclipse, or VS Code)
- Git
- Minecraft test server (Spigot/Paper 1.21+)

### Setting Up

```bash
# Clone the repository
git clone <repository-url>
cd HalalCraft

# Build the project
mvn clean package

# JAR will be in target/HalalCraft-1.21.11.jar
```

### Project Structure

```
HalalCraft/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── me/halalcraft/
│   │   │       ├── HalalCraft.java          # Main plugin class
│   │   │       ├── listener/                # Event listeners
│   │   │       │   ├── PrayListener.java
│   │   │       │   ├── DuaListener.java
│   │   │       │   ├── IslamicCombatRule.java
│   │   │       │   ├── AnvilPurificationListener.java
│   │   │       │   ├── VirtueShopSignListener.java
│   │   │       │   ├── UpgradeListener.java
│   │   │       │   ├── DailyChallengeListener.java
│   │   │       │   ├── VirtueShopListener.java
│   │   │       │   ├── PrayerWarningListener.java
│   │   │       │   └── MosqueListener.java
│   │   │       └── mosque/
│   │   │           └── MosqueManager.java
│   │   └── resources/
│   │       ├── config.yml                   # Main configuration
│   │       ├── plugin.yml                   # Plugin metadata
│   │       └── upgrade.yml                  # Enchantment prices
│   └── test/
│       └── java/                            # Unit tests (to be added)
├── pom.xml                                  # Maven configuration
├── README.md                                # Documentation
├── CHANGELOG.md                             # Version history
└── CONTRIBUTING.md                          # This file
```

## 🎯 Coding Standards

### Java Style Guidelines

1. **Naming Conventions**
   - Classes: `PascalCase`
   - Methods: `camelCase`
   - Variables: `camelCase`
   - Constants: `UPPER_SNAKE_CASE`

2. **Code Organization**
   - Keep methods focused and small
   - Use meaningful variable names
   - Comment complex logic
   - Follow existing patterns in the codebase

3. **Event Listeners**
   - Place in `listener/` package
   - Use `@EventHandler` annotation
   - Handle null checks properly
   - Cancel events when appropriate

4. **Configuration**
   - Use config.yml for user-configurable values
   - Provide sensible defaults
   - Document config options

### Example Code Style

```java
public class ExampleListener implements Listener {

    private final HalalCraft plugin;

    public ExampleListener(HalalCraft plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerAction(PlayerEvent event) {
        Player player = event.getPlayer();
        
        // Null check
        if (player == null) {
            return;
        }

        // Get config value
        int virtueGain = plugin.getConfig().getInt("economy.virtue.action", 5);
        
        // Update player virtue
        plugin.changeVirtue(player, virtueGain);
        
        // Send message
        player.sendMessage("§a✓ Earned " + virtueGain + " virtue!");
    }
}
```

## 🔄 Pull Request Process

1. **Fork the Repository**
2. **Create a Feature Branch**
   ```bash
   git checkout -b feature/my-new-feature
   ```

3. **Make Your Changes**
   - Write clean, documented code
   - Follow coding standards
   - Test thoroughly

4. **Commit Your Changes**
   ```bash
   git commit -m "Add feature: description"
   ```
   - Use clear, descriptive commit messages
   - Reference issue numbers if applicable

5. **Push to Your Fork**
   ```bash
   git push origin feature/my-new-feature
   ```

6. **Submit Pull Request**
   - Provide clear description
   - List changes made
   - Reference related issues
   - Add screenshots if UI changes

## ✅ Testing Guidelines

Before submitting:

1. **Build Test**
   ```bash
   mvn clean package
   ```
   - Ensure no compilation errors
   - Check for warnings

2. **Functionality Test**
   - Test on a local server
   - Verify new features work
   - Check existing features still work
   - Test edge cases

3. **Configuration Test**
   - Ensure config.yml updates work
   - Test with default values
   - Test with custom values

## 📝 Documentation

When adding features:

1. **Update README.md**
   - Add to features list
   - Document new commands
   - Update configuration section

2. **Update CHANGELOG.md**
   - Add to "Unreleased" section
   - Describe changes clearly

3. **Code Comments**
   - Document complex logic
   - Explain Islamic concepts if needed
   - Add JavaDoc for public methods

## 🌟 Feature Guidelines

### Islamic Authenticity

- Ensure features align with Islamic teachings
- Consult Islamic sources when needed
- Be respectful of Islamic practices
- Avoid controversial interpretations

### Gameplay Balance

- Keep features balanced and fair
- Avoid pay-to-win mechanics
- Maintain skill-based gameplay
- Consider server performance

### User Experience

- Make features intuitive
- Provide clear feedback
- Use color-coded messages
- Handle errors gracefully

## 🎨 Message Formatting

Use Minecraft color codes consistently:

- `§a` Green - Success messages
- `§c` Red - Error messages
- `§e` Yellow - Warnings/Info
- `§b` Aqua - Values/Numbers
- `§f` White - Neutral text
- `§l` Bold - Emphasis

Example:
```java
player.sendMessage("§a✓ Prayer completed! Earned §b5§a virtue!");
player.sendMessage("§cYou must be in a mosque!");
player.sendMessage("§eWarning: Low virtue balance");
```

## 🔐 Security

- Never store sensitive data in plain text
- Validate all user input
- Use prepared statements for any database operations
- Sanitize file paths
- Check permissions before operations

## 📚 Resources

- [Spigot API JavaDocs](https://hub.spigotmc.org/javadocs/spigot/)
- [Paper API Docs](https://docs.papermc.io/)
- [Bukkit Wiki](https://bukkit.fandom.com/wiki/Main_Page)

## 🤝 Community

- Be welcoming to new contributors
- Help others learn
- Share knowledge
- Collaborate respectfully

## ❓ Questions?

If you have questions about contributing:
- Open an issue for discussion
- Ask in pull request comments
- Contact maintainers

---

**Thank you for contributing to HalalCraft! May your contributions be rewarded! 🤲**
