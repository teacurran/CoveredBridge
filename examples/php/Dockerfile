# Use the official PHP 7.4 Apache image from Docker Hub
FROM php:7.4-apache

# Copy the PHP files from your machine into the container
COPY . /var/www/html/

# Use the default production configuration
RUN mv "$PHP_INI_DIR/php.ini-production" "$PHP_INI_DIR/php.ini"

# Enable Apache mod_rewrite
RUN a2enmod rewrite

# Change the ownership of the /var/www/html/ directory to www-data
RUN chown -R www-data:www-data /var/www/html/

# Expose port 80 for the app
EXPOSE 80

